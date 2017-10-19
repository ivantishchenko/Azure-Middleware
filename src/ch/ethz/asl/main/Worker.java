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

    private void read(SelectionKey key) throws IOException {
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

                if(request.getType() == Request.RequestType.SET) {
                    writeAll(request);
                }
                else if (request.getType() == Request.RequestType.GET) {
                    writeOne(request);
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
                            read(key);
                        }
                        it.remove();


                        // reply to client
                        if ( responsesLeft == 0 ) {
                            System.out.println("Set size: " + serverResponses.size());

                            buffer.clear();
                            // forward the first response to client


                            buffer.put(InputValidator.getSingleResponse(serverResponses));
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

//    private void printJobQueue() {
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("RequestQueue is now. Thread # " + Thread.currentThread().getId());
//        jobQueue.stream().forEach(x -> {
//            System.out.println(x.getRequestMessage());
//        });
//    }


}