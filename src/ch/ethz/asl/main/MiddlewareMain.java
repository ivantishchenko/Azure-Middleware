package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class MiddlewareMain {
    // Logging
    private static final Logger log = LogManager.getLogger(MiddlewareMain.class);
    private static final Logger instrumentationLog = LogManager.getLogger("stat_file");

    // command line args
    public String mwIP;
    public int mwPort;
    public List<String> memCachedServers;
    public int workersNumber;
    public static boolean sharedRead;
    public static int serversNum;

    // internal params
    public LinkedBlockingQueue<Request> requestQueue;
    public NetThread netThread;
    public List<Worker> workersPool;
    public CycleCounter counterRR;

    public MiddlewareMain(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded) {
        // command line arguments
        mwIP = myIp;
        mwPort = myPort;
        memCachedServers = mcAddresses;
        workersNumber = numThreadsPTP;
        sharedRead = readSharded;
        serversNum = memCachedServers.size();

        //internal params
        requestQueue = new LinkedBlockingQueue<>();
        netThread = new NetThread(requestQueue, myIp, myPort);
        workersPool = new ArrayList<>(workersNumber);
        
        counterRR = new CycleCounter(memCachedServers.size());
    }

    public void run() {
        // start netThread
        netThread.setName("NetThread");
        netThread.start();
        // start workers
        for (int i = 0; i < workersNumber; i++) {
            Worker worker = new Worker(requestQueue, counterRR, memCachedServers);
            worker.setName("Worker " + String.valueOf(i + 1));
            workersPool.add(worker);

            worker.start();
        }

        // instrumentation part
        //doInstrumentation();
        doKillHook();
    }

//    private void doInstrumentation() {
//        int initialDelay = 2000; // start after 2 seconds
//        int period = 3000;        // repeat every 5 seconds
//
//        Timer timer = new Timer();
//        TimerTask task = new TimerTask() {
//
//            double prev;
//            public void run() {
//                //instrumentationLog.info(String.format("%d %s %d", "hello", 1,2));
//
//                double avgThroughput = 0;
//
//                for (Worker w: workersPool) avgThroughput += w.getStatistics().getThroughput();
//                avgThroughput /= workersPool.size();
//                instrumentationLog.info(String.format("%f %f %f", avgThroughput, 1.0 ,2.0));
//
//
//                for (Worker w: workersPool) w.getStatistics().setJobCount(0);
//            }
//        };
//
//        timer.scheduleAtFixedRate(task, initialDelay, period);
//    }

    private void doKillHook() {
        // hook statistics
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Middleware shuts down...");
            System.out.println("Final aggregates");

            // Agregation
        }));

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
