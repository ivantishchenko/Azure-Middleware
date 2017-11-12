package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

        addKillHook();
    }

    private void addKillHook() {
        // hook statistics
        Thread hook = new ShutDownHook(workersPool);
        Runtime.getRuntime().addShutdownHook(hook);
    }

}
