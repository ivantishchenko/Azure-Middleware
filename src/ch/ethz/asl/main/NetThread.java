package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class NetThread extends Thread {
    // logging
    private final static Logger log = LogManager.getLogger(NetThread.class);

    public static final int MESSAGE_SIZE = 4096;
    public Selector selector;
    public ByteBuffer buffer;
    public LinkedBlockingQueue<Request> requestQueue;

    private String middlewareIP;
    private int middlewarePort;

    private int arrivalRate = 0;
    private List<Integer> arrivalRateValues;
    private AtomicInteger arrivalRateCounter;


    public List<Integer> getArrivalRateValues() {
        return arrivalRateValues;
    }

    public NetThread(LinkedBlockingQueue<Request> reqQueue, String ip, int port) {
        this.middlewarePort = port;
        this.middlewareIP = ip;
        // buffers for socket
        buffer = ByteBuffer.allocate(MESSAGE_SIZE);
        //Queue
        requestQueue = reqQueue;

        arrivalRateValues = new ArrayList<>();
        arrivalRateCounter = new AtomicInteger();
    }

    @Override
    public void run() {
        log.info(Thread.currentThread().getName() + " started");
        try {
            serve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serve() throws IOException {
        selector = Selector.open();

        // Listen to conn on the socket
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        InetSocketAddress addr = new InetSocketAddress(this.middlewareIP, this.middlewarePort);
        serverChannel.bind(addr);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);


        doInstrumentation();
        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();

            while (it.hasNext()) {

                SelectionKey key = it.next();
                if (key.isAcceptable()) {
                    // channel is ready for accepting
                    accept(key);
                } else if (key.isReadable()) {
                    // Channel is ready for reading
                    read(key);
                    //arrivalRate++;
                    arrivalRateCounter.incrementAndGet();
                }
                it.remove();

            }
        }

    }

    private void accept(SelectionKey key) throws IOException {
        // make a new channel read
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        log.info("accepted connection");
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

            // Convert to string

            byte[] message = new byte[numBytesRead];
            System.arraycopy(buffer.array(), 0, message, 0, numBytesRead);
            buffer.clear();

            // creare Request object and queue it
            Request request = new Request(channel, message);
            // parse request GET SET
            Parser.classifyRequest(request);


            // instrumentation Time entered the queue
            request.setEnterQueueTime(System.nanoTime());
            //requestQueue.add(request);
            try {
                requestQueue.put(request);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void doInstrumentation() {
        int initialDelay = 0; // start after 1 second
        int period = (int) (Statistics.testInterval * 1000);        // repeat every N seconds

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {

            public void run() {
                // new job count - prev job count gives job count in the interval

                //arrivalRateValues.add(arrivalRate);
                //arrivalRate = 0;
                arrivalRateValues.add(arrivalRateCounter.getAndSet(0));
            }
        };

        timer.scheduleAtFixedRate(task, initialDelay, period);
    }



}
