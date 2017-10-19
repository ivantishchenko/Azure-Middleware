package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class MiddlewareMain {
    // Logging
    private final static Logger log = LogManager.getLogger(MiddlewareMain.class);

    // command line args
    public String mwIP;
    public int mwPort;
    public List<String> memCachedServers;
    public int workersNumber;
    public boolean sharedRead;

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

        //internal params
        requestQueue = new LinkedBlockingQueue<>();
        netThread = new NetThread(requestQueue, myIp, myPort);
        workersPool = new ArrayList<>(workersNumber);

        //
        System.out.println(memCachedServers.size());
        counterRR = new CycleCounter(memCachedServers.size());
    }

    public void run() {
        // start netThread
        netThread.start();


        // start workers
        for (int i = 0; i < workersNumber; i++) {
            Worker worker = new Worker(requestQueue, counterRR, memCachedServers);
            workersPool.add(worker);

            worker.start();
        }

    }

}
