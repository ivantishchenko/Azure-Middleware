package ch.ethz.asl.main;

import com.sun.org.apache.regexp.internal.RE;
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
import java.util.stream.Stream;

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
    private final int MESSAGE_SIZE = 32768;
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

    private String[] serverResponsesGlue;
    private ByteBuffer[] byteBuffersGlue;

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

        serverResponsesGlue = new String[serversNumber];
        for (int i = 0; i < serverResponsesGlue.length; i++) {
            serverResponsesGlue[i] = "";
        }

        byteBuffersGlue = new ByteBuffer[serversNumber];
        for (int i = 0; i < byteBuffersGlue.length; i++) {
            byteBuffersGlue[i] = ByteBuffer.allocate(MESSAGE_SIZE);
            byteBuffersGlue[i].limit(MESSAGE_SIZE);
        }


        for (int i = 0; i < serversNumber; i++) {
            statistics.getEqualLoadHistogram().put(i, 0);
        }

    }

    private void openServerConnections() throws IOException {

        for (String mcAddress: servers) {
            String serverIP = mcAddress.split(":")[0];
            int serverPort = Integer.parseInt(mcAddress.split(":")[1]);

            SocketChannel chan = SocketChannel.open();
            serverSocketChannels.add(chan);
            chan.configureBlocking(true);
            chan.connect(new InetSocketAddress(serverIP, serverPort));
            // wait until all connections are open
            while (!chan.finishConnect()) {
                continue;
            }
        }

    }

    private int writeOne(Request request) {
        responsesLeft = 0;

        int serverIdx = roundRobinCounter.getCount();
        SocketChannel serverChannel = serverSocketChannels.get(serverIdx);

        buffer.clear();

        //System.out.println(Thread.currentThread().getId() + " Going to send Request to Server: " + new String(request.getRawMessage()));
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

        statistics.getEqualLoadHistogram().put(serverIdx, statistics.getEqualLoadHistogram().get(serverIdx) + 1);

        return serverIdx;
    }

    private void writeAll(Request request) {
        responsesLeft = 0;

        // take one Request from head of the queue
        try {
            // Replicate to all Servers
            for (SocketChannel serverChannel: serverSocketChannels) {
                buffer.clear();
                //System.out.println(Thread.currentThread().getId() + " Going to send to server: " + new String(request.getRawMessage()));
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

                statistics.getEqualLoadHistogram().put(i, statistics.getEqualLoadHistogram().get(i) + 1);

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

    private void read(SocketChannel channel, Request r) throws IOException {
        buffer.clear();
        int numBytesRead = channel.read(buffer);

        if (numBytesRead == -1) {
            channel.close();
            buffer.clear();
        } else {

            if (numBytesRead == 0)  log.error("Zero bytes read");

            // Convert to string
            byte[] message = new byte[numBytesRead];
            System.arraycopy(buffer.array(), 0, message, 0, numBytesRead);


            if (Parser.isEmptyResponse(message)) statistics.setCacheMissCount(statistics.getCacheMissCount() + 1);

            // TEST


            //System.out.println("Num bytes read " + numBytesRead);
            //System.out.println("Weird responce " + new String(message));
            //System.out.println("Got from port " + channel.socket().getPort());


            // TEST

            serverResponses.add(ByteBuffer.wrap(message));

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

    private void readGet(SelectionKey key, Request r) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        String addr = channel.socket().getInetAddress().getHostAddress()+":"+ Integer.toString(channel.socket().getPort());
        int idx = serverIdx.get(addr);

        buffer.clear();
        int numBytesRead = channel.read(buffer);

        if (numBytesRead == -1) {
            key.cancel();
            channel.close();
            buffer.clear();
        } else {

            if (numBytesRead == 0)  log.error("Zero bytes read");

            // Convert to string
            byte[] message = new byte[numBytesRead];
            System.arraycopy(buffer.array(), 0, message, 0, numBytesRead);


            if (Parser.isEmptyResponse(message)) statistics.setCacheMissCount(statistics.getCacheMissCount() + 1);

            // TEST

//            System.out.println("Num bytes read " + numBytesRead);
//            System.out.println("Weird responce " + new String(message));
//            System.out.println("Got from port " + channel.socket().getPort());

            String checkString = new String(message);
            if ( checkString.contains("END\r\n") ) {

                serverResponsesGlue[idx] += checkString;
                //System.out.println("GLUED RESPONSE " + serverResponsesGlue[idx]);
                serverResponses.add(ByteBuffer.wrap(serverResponsesGlue[idx].getBytes()));

                if (MiddlewareMain.sharedRead && r.getType() == Request.RequestType.MULTI_GET) {

                    sharedResponces.set(idx, serverResponsesGlue[idx].getBytes());
                }

                serverResponsesGlue[idx] = "";
                log.info("Reply message: " + new String(serverResponsesGlue[idx].getBytes()));
                // responded
                responsesLeft--;
            } else {
                serverResponsesGlue[idx] += checkString;
            }
        }

        // if all servers responded
        // try to forward back to client

        log.info("Reply from: " + addr);

    }

    private byte[] readAll(int serverIdx) {
        byte[] msg;
        String checkString;
        int totalRead = 0;
        do {
            //System.out.println("GOIN TO READ");
            int numBytesRead = 0;
            try {
                numBytesRead = serverSocketChannels.get(serverIdx).read(byteBuffersGlue[serverIdx]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            totalRead += numBytesRead;
            //System.out.println("Num bytes READ " + numBytesRead);
            msg = new byte[totalRead];
            System.arraycopy(byteBuffersGlue[serverIdx].array(), 0, msg, 0, totalRead);
            checkString = new String(msg);
            //System.out.println("Glued response : " + checkString);
        } while (!checkString.contains("END\r\n"));
        return msg;
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
                request.setQueueWaitTime(request.getLeaveQueueTime() - request.getEnterQueueTime());
                //instrumentationLog.info("Job waited for: " + request.getQueueWaitTime());

                statistics.setQueueWaitTime(statistics.getQueueWaitTime() + request.getQueueWaitTime());
                statistics.setJobCount(statistics.getJobCount() + 1);
                statistics.setQueueLength(jobQueue.size());

                // send a job to servers
                int serverIdx = 0;
                switch (request.getType()) {
                    case SET:
                        writeAll(request);
                        statistics.setSETCount(statistics.getSETCount() + 1);
                        break;
                    case GET:
                        serverIdx = writeOne(request);
                        statistics.setGETCount(statistics.getGETCount() + 1);
                        break;
                    case MULTI_GET:
                        if (MiddlewareMain.sharedRead) writeSplit(request, serversNumber);
                        else serverIdx = writeOne(request);
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
                int numBytesRead = 0;
                //System.out.println("TYPE is " + request.getType());
                switch (request.getType()) {
                    case SET:
                        Set<ByteBuffer> setResponses = new HashSet<>();
                        for (int i = 0; i < serversNumber; i++) {
                            numBytesRead = serverSocketChannels.get(i).read(byteBuffersGlue[i]);

                            //byte[] message = new byte[numBytesRead];
                            //System.arraycopy(byteBuffersGlue[i].array(), 0, message, 0, numBytesRead);

                            if (numBytesRead == -1) serverSocketChannels.get(i).close();
                            else {
                                setResponses.add(byteBuffersGlue[i]);

                                byte[] message = new byte[numBytesRead];
                                System.arraycopy(byteBuffersGlue[i].array(), 0, message, 0, numBytesRead);
                                String checkStr = new String(message);
                                if (checkStr.toLowerCase().contains("error")) {
                                    serverIdx = i;
                                }
                            }

                            //System.out.println("REPLY IS = " + new String(message));

                        }
                        break;
                    case GET:
                        numBytesRead = serverSocketChannels.get(serverIdx).read(byteBuffersGlue[serverIdx]);
                        if ( numBytesRead == -1 ) serverSocketChannels.get(serverIdx).close();
                        else {
                            byte[] message = new byte[numBytesRead];
                            System.arraycopy(byteBuffersGlue[serverIdx].array(), 0, message, 0, numBytesRead);
                            if (Parser.isEmptyResponse(message)) statistics.setCacheMissCount(statistics.getCacheMissCount() + 1);
                        }
                        break;
                    case MULTI_GET:
                        if (MiddlewareMain.sharedRead) {
                            ArrayList<byte[]> responses= new ArrayList<>();
                            for (int i = 0; i < serversNumber; i++) {
                                byte[] msg = readAll(i);
                                responses.add(msg);

                            }
                            byte[] out = Parser.combineSplitResponses(responses);
                            byteBuffersGlue[serverIdx].clear();
                            byteBuffersGlue[serverIdx].put(out);
                        }
                        else {
                            readAll(serverIdx);
                        }
                        break;
                    default:
                        break;
                }


                // reply to client
                //System.out.println("0 RESPONSES");
                long endServiceTime = System.nanoTime();
                // instrumentation
                long serviceTime = endServiceTime - startServiceTime;
                statistics.setServiceTime(statistics.getServiceTime() + serviceTime);


                //byte[] msg = new byte[numBytesRead];
                //System.arraycopy(byteBuffersGlue[serverIdx].array(), 0, msg, 0, numBytesRead);
                //System.out.println("TO CLIENT " + new String(msg));
                byteBuffersGlue[serverIdx].flip();

                while (byteBuffersGlue[serverIdx].hasRemaining()) {
                    request.getRequestChan().write(byteBuffersGlue[serverIdx]);
                }

                for (int i = 0; i < serversNumber ; i++) {
                    byteBuffersGlue[i].clear();
                }

                // repsonse time histrogram
                long sendBackClientTime = System.nanoTime();
                long responseTime = sendBackClientTime - request.getEnterQueueTime();

                statistics.addResponseTime(responseTime);
                statistics.setLatency(statistics.getLatency() + responseTime);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void cleanGlueResponses() {
        for (int i = 0; i < serverResponsesGlue.length; i++) {
            serverResponsesGlue[i] = "";
        }
    }

    private void doInstrumentation() {
        int initialDelay = 1000; // start after 1 second
        int period = (int) (Statistics.testInterval * 1000);        // repeat every N seconds

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {

            public void run() {
                // new job count - prev job count gives job count in the interval
                int jobCount = statistics.getJobCount();

                int throughput = jobCount / Statistics.testInterval;

                int queueLength = jobQueue.size();
                int getCount = statistics.getGETCount();
                int setCount = statistics.getSETCount();
                int multiGetCount = statistics.getMULTIGETCount();
                long queueWaitTime = statistics.getQueueWaitTime() / Math.max(jobCount, 1);
                long serviceTime = statistics.getServiceTime() / Math.max(jobCount, 1);
                long latency = statistics.getLatency() / Math.max(jobCount, 1);
                // to msec

                if ( jobCount != 0) {
                    instrumentationLog.info(String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d", logName, throughput , queueLength, queueWaitTime, serviceTime, setCount, getCount, multiGetCount, latency));
                    //instrumentationLog.info(String.format("%s,%d,%d,%d,%d,%d,%d,%d", logName, throughput , queueLength, queueWaitTime, serviceTime, setCount, getCount, multiGetCount));

                    synchronized (timer) {
                        statistics.setJobCount(0);
                        statistics.setGETCount(0);
                        statistics.setSETCount(0);
                        statistics.setMULTIGETCount(0);
                        statistics.setQueueWaitTime(0);
                        statistics.setServiceTime(0);
                        statistics.setLatency(0);
                    }
                }


            }
        };

        timer.scheduleAtFixedRate(task, initialDelay, period);
    }

    public Statistics getStatistics() {
        return statistics;
    }

}
