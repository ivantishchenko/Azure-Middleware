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

public class Worker extends Thread {
    // Logging
    private final static Logger log = LogManager.getLogger(Worker.class);

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

        ArrayList<byte[]> splitRequests = InputValidator.splitRequest(request, serversNumber);
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

            log.error("Weird responce " + new String(message));

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
        log.info("Worker #" + Thread.currentThread().getId() + " started");

        try {
            // open connections to servers
            openServerConnections();

            // busy wait for jobs
            while(true) {
                // block until there are jobs available
                // blocking method
                Request request = jobQueue.take();
                // send a job to servers

                switch (request.getType()) {
                    case SET:
                        writeAll(request);
                        break;
                    case GET:
                        writeOne(request);
                        break;
                    case MULTI_GET:
                        if (MiddlewareMain.sharedRead) writeSplit(request, serversNumber);
                        else writeOne(request);
                        break;
                    default:
                        break;
                }

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
                                    buffer.put(InputValidator.getSingleResponse(serverResponses));
                                    break;
                                case GET:
                                    buffer.put(serverResponses.iterator().next());
                                    break;
                                case MULTI_GET:
                                    if (MiddlewareMain.sharedRead) {
                                        //byte[] out = InputValidator.combineSplits(serverResponses);
                                        //buffer.put(out);
                                        String test = new String(InputValidator.combineSplitResponses(sharedResponces));
                                        log.error("Got weird reply : " + test);
                                        buffer.put(InputValidator.combineSplitResponses(sharedResponces));
                                    }
                                    else buffer.put(serverResponses.iterator().next());
                                    break;
                                default:
                                    break;
                            }

//                            if (MiddlewareMain.sharedRead && request.getType() == Request.RequestType.MULTI_GET) {
//                                byte[] out = InputValidator.combineSplits(serverResponses);
//                                buffer.put(out);
//                            }
//                            else buffer.put(InputValidator.getSingleResponse(serverResponses));

                            buffer.flip();

                            while (buffer.hasRemaining()) {
                                request.getRequestChan().write(buffer);
                            }

                            buffer.clear();
                            serverResponses.clear();

                        }

                    }
                    // wait for responses from all servers
                    // use a semaphore to check the completion of requests
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
