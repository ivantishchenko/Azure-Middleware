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
    private ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_SIZE);
    // counter of jobs waiting for reply
    private int responcesLeft;



    public Worker(LinkedBlockingQueue<Request> reqQueue, List<String> memCachedServers) {
        // params
        servers = memCachedServers;
        jobQueue = reqQueue;
        serversNumber = servers.size();

        // architecture
        buffer = ByteBuffer.allocate(MESSAGE_SIZE);
        serverSocketChannels = new ArrayList<>();
        responcesLeft = 0;
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

    private void write(Request request) {
        responcesLeft = 0;

        // take one Request from head of the queue
        try {
            // Replicate to all Servers
            for (SocketChannel serverChannel: serverSocketChannels) {
                buffer.clear();
                System.out.println(Thread.currentThread().getId() + " Going to send: " + new String(request.getRawMessage()));
                buffer.put(request.getRawMessage());
                buffer.flip();

                while (buffer.hasRemaining()) {
                    serverChannel.write(buffer);
                }
                buffer.clear();

                // increase jobs counter
                responcesLeft++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int numBytesRead = channel.read(buffer);
        if (numBytesRead == -1) {
            // Client closed connecting
            key.cancel();
            channel.close();
            buffer.clear();
        } else {

            if (numBytesRead == 0) System.out.println("Zero bytes read");

            // Convert to string
            byte[] message = new byte[numBytesRead];
            System.arraycopy(buffer.array(), 0, message, 0, numBytesRead);


            System.out.println("Reply from SERVER: " + new String(message));
            buffer.clear();
            // responded
            responcesLeft--;
        }

        String addr = channel.socket().getInetAddress().getHostAddress()+":"+ Integer.toString(channel.socket().getPort());
        System.out.println("Reply from: " + addr);

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
                Request request = jobQueue.take();
                // send a job to servers
                write(request);

                // block until there is something to read
                // wait for
                while ( responcesLeft > 0 ) {
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
