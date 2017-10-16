package ch.ethz.asl.main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class MiddlewareMain {
    // command line args
    public String mwIP;
    public int mwPort;
    public List<String> memCachedServers;
    public int workersNumber;
    public boolean sharedRead;

    // internal params
    public LinkedBlockingQueue<Request> requestQueue;
    public NetThread conManager;
    public List<Worker> workers;

    public MiddlewareMain(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded) {
        // command line arguments
        mwIP = myIp;
        mwPort = myPort;
        memCachedServers = mcAddresses;
        workersNumber = numThreadsPTP;
        sharedRead = readSharded;

        //internal params
        requestQueue = new LinkedBlockingQueue<Request>();
        conManager = new NetThread(requestQueue, myIp, myPort);
        workers = new ArrayList<Worker>(workersNumber);
    }

    public void run() {
        conManager.start();
        System.out.println("Started MiddlewareMain");

        for (int i = 0; i < workersNumber; i++) {
            Worker worker = new Worker(requestQueue, memCachedServers);
            workers.add(worker);

            worker.start();
            System.out.println("Started a worker");
        }

    }

}
