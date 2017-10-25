package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker extends Thread {
    // Logging
    private static final Logger log = LogManager.getLogger(Worker.class);
    private static final Logger instrumentationLog = LogManager.getLogger("stat_file");

    // internal params
    public List<String> servers;
    public LinkedBlockingQueue<Request> jobQueue;
    public int serversNumber;

    // architecture
    private Selector selector;
    private List<SocketChannel> serverSocketChannels;
    private final int MESSAGE_SIZE = 8092;
    private ByteBuffer buffer;

    // counter of jobs waiting for reply
    private int responsesLeft;
    private Set<ByteBuffer> serverResponses;
    private CycleCounter roundRobinCounter;

    // arch 2
    // maps server address to IDX
    private HashMap<String, Integer> serverIdx;
    private ArrayList<byte[]> sharedResponces;

    //unsupported operations
    int invalidOperationCount;

    //Instrumentation object
    private Statistics statistics;
    private String logName;

    public Worker(LinkedBlockingQueue<Request> reqQueue, CycleCounter counter, List<String> memCachedServers) {
        // params
        servers = memCachedServers;
        jobQueue = reqQueue;
        serversNumber = servers.size();

        // architecture
        buffer = ByteBuffer.allocate(MESSAGE_SIZE);
        serverSocketChannels = new ArrayList<>();
        responsesLeft = 0;
        serverResponses = new HashSet<>();

        // Round robin
        roundRobinCounter = counter;

        // SHared get

        serverIdx = new HashMap<String, Integer>();
        sharedResponces = new ArrayList<>(serversNumber);

        for ( int i = 0; i < serversNumber; i++) {
            serverIdx.put(memCachedServers.get(i), i);
            sharedResponces.add(i, "".getBytes());
        }

        // invalid request
        invalidOperationCount = 0;

        // Statitics
        statistics = new Statistics();
    }

    private void openServerConnections() throws IOException {
        selector = Selector.open();

        for (String mcAddress: servers) {
            String serverIP = mcAddress.split(":")[0];
            int serverPort = Integer.parseInt(mcAddress.split(":")[1]);

            SocketChannel chan = SocketChannel.open();
            serverSocketChannels.add(chan);
            chan.configureBlocking(false);
            chan.connect(new InetSocketAddress(serverIP, serverPort));
            chan.register(this.selector, SelectionKey.OP_READ);

            // wait until all connections are open
            while (!chan.finishConnect()) {
                continue;
            }
        }

    }

    private void writeOne(Request request) {
        responsesLeft = 0;

        int serverIdx = roundRobinCounter.getCount();
        SocketChannel serverChannel = serverSocketChannels.get(serverIdx);

        buffer.clear();
        log.info(Thread.currentThread().getId() + " Going to send: " + new String(request.getRawMessage()));
        buffer.put(request.getRawMessage());
        buffer.flip();

        while (buffer.hasRemaining()) {
            try {
                serverChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        buffer.clear();

        roundRobinCounter.increment();
        responsesLeft++;
    }

    private void writeAll(Request request) {
        responsesLeft = 0;

        // take one Request from head of the queue
        try {
            // Replicate to all Servers
            for (SocketChannel serverChannel: serverSocketChannels) {
                buffer.clear();
                log.info(Thread.currentThread().getId() + " Going to send: " + new String(request.getRawMessage()));
                buffer.put(request.getRawMessage());
                buffer.flip();

                while (buffer.hasRemaining()) {
                    serverChannel.write(buffer);
                }
                buffer.clear();

                // increase jobs counter
                responsesLeft++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSplit(Request request, int serversNumber) {

        ArrayList<byte[]> splitRequests = Parser.splitRequest(request, serversNumber);
        responsesLeft = 0;

        try {
            // Replicate to all Servers
            // Loop over non empty splits
            for (int i = 0; i < splitRequests.size(); i++) {

                byte[] out = splitRequests.get(i);
                log.info(Thread.currentThread().getId() + " Going to send: " + new String(out));

                buffer.clear();
                buffer.put(out);
                buffer.flip();

                while (buffer.hasRemaining()) {
                    serverSocketChannels.get(i).write(buffer);
                }
                buffer.clear();

                // increase jobs counter
                responsesLeft++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void read(SelectionKey key, Request r) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        buffer.clear();
        int numBytesRead = channel.read(buffer);

        if (numBytesRead == -1) {
            // Client closed connecting
            key.cancel();
            channel.close();
            buffer.clear();
        } else {

            if (numBytesRead == 0)  log.error("Zero bytes read");

            // Convert to string
            byte[] message = new byte[numBytesRead];
            System.arraycopy(buffer.array(), 0, message, 0, numBytesRead);


            serverResponses.add(ByteBuffer.wrap(message));

            //log.error("Weird responce " + new String(message));

            if (MiddlewareMain.sharedRead && r.getType() == Request.RequestType.MULTI_GET) {
                String addr = channel.socket().getInetAddress().getHostAddress()+":"+ Integer.toString(channel.socket().getPort());
                int idx = serverIdx.get(addr);
                sharedResponces.set(idx, message);
            }

            log.info("Reply message: " + new String(message));
            buffer.clear();
            // responded
            responsesLeft--;
        }

        // if all servers responded
        // try to forward back to client

        String addr = channel.socket().getInetAddress().getHostAddress()+":"+ Integer.toString(channel.socket().getPort());
        log.info("Reply from: " + addr);

    }



    @Override
    public void run() {
        logName = Thread.currentThread().getName();
        log.info(logName + " started");

        // ISTRUMENTATION
        doInstrumentation();

        try {
            // open connections to servers
            openServerConnections();
            // busy wait for jobs
            while(true) {
                // block until there are jobs available
                // blocking method
                Request request = jobQueue.take();

                //Instrumentation
                request.setLeaveQueueTime(System.nanoTime());
                // store in miliseconds
                request.setQueueWaitTime((request.getLeaveQueueTime() - request.getEnterQueueTime()) / 1000000);
                //instrumentationLog.info("Job waited for: " + request.getQueueWaitTime());

                statistics.setQueueWaitTime(statistics.getQueueWaitTime() + request.getQueueWaitTime());
                statistics.setJobCount(statistics.getJobCount() + 1);
                statistics.setQueueLength(jobQueue.size());

                // send a job to servers

                switch (request.getType()) {
                    case SET:
                        writeAll(request);
                        statistics.setSETCount(statistics.getSETCount() + 1);
                        break;
                    case GET:
                        writeOne(request);
                        statistics.setGETCount(statistics.getGETCount() + 1);
                        break;
                    case MULTI_GET:
                        if (MiddlewareMain.sharedRead) writeSplit(request, serversNumber);
                        else writeOne(request);
                        statistics.setMULTIGETCount(statistics.getMULTIGETCount() + 1);
                        break;
                    case UNSUPPORTED:
                        invalidOperationCount++;
                        log.info("# of invalid operations: " + invalidOperationCount);
                        break;
                    default:
                        break;
                }
                long startServiceTime = System.nanoTime();

                // block until there is something to read
                // wait for all responses
                while ( responsesLeft > 0 ) {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {

                        SelectionKey key = it.next();
                        if (key.isReadable()) {
                            // Channel is ready for reading
                            read(key, request);
                        }
                        it.remove();


                        // reply to client
                        if ( responsesLeft == 0 ) {
                            //System.out.println("Set size: " + serverResponses.size());

                            buffer.clear();
                            // forward the first response to client


                            switch (request.getType()) {
                                case SET:
                                    buffer.put(Parser.getSingleResponse(serverResponses));
                                    break;
                                case GET:
                                    buffer.put(serverResponses.iterator().next());
                                    break;
                                case MULTI_GET:
                                    if (MiddlewareMain.sharedRead) {
                                        byte[] out = Parser.combineSplitResponses(sharedResponces);
                                        buffer.put(out);
                                    }
                                    else {
                                        buffer.put(serverResponses.iterator().next());
                                    }
                                    break;
                                default:
                                    break;
                            }

                            buffer.flip();

                            while (buffer.hasRemaining()) {
                                request.getRequestChan().write(buffer);
                            }

                            buffer.clear();

                            // CLEAR RESPONSES AFTER GET!!!
                            serverResponses.clear();

                            // shared responces clear!!!!
                            for ( int i = 0; i < serversNumber; i++) sharedResponces.set(i, "".getBytes());

                        }

                    }
                    // wait for responses from all servers
                    // use a semaphore to check the completion of requests
                }

                long serviceTime = (System.nanoTime() - startServiceTime ) / 1000000;
                statistics.setServiceTime(statistics.getServiceTime() + serviceTime);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void doInstrumentation() {
        int initialDelay = 1000; // start after 2 seconds
        int period = (int) (Statistics.testInterval * 1000);        // repeat every N seconds

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {

            int prevJobCount = 0;
            int prevSETCount = 0;
            int prevGETCount = 0;
            int prevMULTIGETCount = 0;
            long prevQueueWaitTime = 0;
            long prevServiceTime = 0;

            public void run() {

                // new job count - prev job count gives job count in the interval
                int jobCount = (statistics.getJobCount() - prevJobCount);

                if (jobCount != 0 ) {
                    double throughput = jobCount / Statistics.testInterval;
                    int queueLength = statistics.getQueueLength();
                    int getCount = statistics.getGETCount() - prevGETCount;
                    int setCount = statistics.getSETCount() - prevSETCount;
                    int multiGetCount = statistics.getMULTIGETCount() - prevMULTIGETCount;

                    long queueWaitTime = (statistics.getQueueWaitTime() - prevQueueWaitTime) / jobCount;
                    long serviceTime = (statistics.getServiceTime() - prevServiceTime) / jobCount;

                    //instrumentationLog.info(String.format("%d %s %d", "hello", 1,2));
                    instrumentationLog.info(String.format("%s,%f,%d,%d,%d,%d,%d,%d", logName, throughput , queueLength, queueWaitTime, serviceTime, setCount, getCount, multiGetCount));
                    //instrumentationLog.info("I am alive");

                    prevJobCount = jobCount;
                    prevGETCount = getCount;
                    prevMULTIGETCount = multiGetCount;
                    prevSETCount = setCount;
                    prevQueueWaitTime = queueWaitTime;
                    prevServiceTime = serviceTime;
                }

            }
        };

        timer.scheduleAtFixedRate(task, initialDelay, period);
    }

    public Statistics getStatistics() {
        return statistics;
    }

}
