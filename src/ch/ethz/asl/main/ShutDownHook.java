package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

// Aggregates the results from Log file
public class ShutDownHook extends Thread{

    private static final Logger log = LogManager.getLogger(ShutDownHook.class);
    public List<Worker> workersPool;
    public NetThread networkThread;

    public ShutDownHook(List<Worker> pool) {
        this.workersPool = pool;
    }

    public ShutDownHook(List<Worker> pool, NetThread nt) {
        this.workersPool = pool;
        this.networkThread = nt;
    }

    public ShutDownHook() {

    }

    @Override
    public void run() {
        // WAIT FOR STATS
        Thread.currentThread().setName("ShutDownHook");

        System.out.println("Middleware shuts down...");
        System.out.println("Smaller resolution statistics are in a log file...");
        System.out.println("Final aggregates per worker:");
        System.out.println("\n");

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream input = classloader.getResourceAsStream("log4j2.properties");

        Properties prop = new Properties();

        try {
            prop.load(input);

            // Agregation
            final String fileName = prop.getProperty("appender.STAT_FILE.fileName");
            String content = new String(Files.readAllBytes(Paths.get(fileName)));

            String[] lines = content.split("\r\n|\r|\n");
            String[] stats = Arrays.copyOfRange(lines, 1, lines.length);

            // STATISTICS LINES

            List<Statistics> statsList = Arrays.stream(stats).map(x -> {
                String[] values = x.split(",");

                String name = values[0];
                int t = Integer.parseInt(values[1]);
                int len = Integer.parseInt(values[2]);
                long wait = Long.parseLong(values[3]);
                long serve = Long.parseLong(values[4]);
                int get =Integer.parseInt(values[5]);
                int set = Integer.parseInt(values[6]);
                int multiget= Integer.parseInt(values[7]);
                long latency= Long.parseLong(values[8]);

                Statistics s = new Statistics(name, t, len, wait, serve, get, set, multiget, latency);
                return s;
            }).collect(Collectors.toList());

            Map<String, List<Statistics>> statsByWorker = statsList.stream().collect(groupingBy(Statistics::getWorkerName));


            ArrayList<Integer> finalT = new ArrayList<>();
            ArrayList<Double> finalR = new ArrayList<>();
            ArrayList<Integer> finalLen = new ArrayList<>();
            ArrayList<Double> finalWait = new ArrayList<>();
            ArrayList<Double> finalServe = new ArrayList<>();
            ArrayList<Integer> finalGets = new ArrayList<>();
            ArrayList<Integer> finalSets = new ArrayList<>();
            ArrayList<Integer> finalMultiGets = new ArrayList<>();

            statsByWorker.forEach((workerName, workerStats) -> {
                System.out.println(workerName);

                int avgT = (int) workerStats.stream().mapToInt(x -> x.getThroughput()).average().getAsDouble();
                int avgLen = (int) workerStats.stream().mapToInt(x -> x.getQueueLength()).average().getAsDouble();
                double avgWait = workerStats.stream().mapToLong(x -> x.getQueueWaitTime()).average().getAsDouble();
                double avgServe = workerStats.stream().mapToLong(x -> x.getServiceTime()).average().getAsDouble();
                int avgGet = (int) workerStats.stream().mapToInt(x -> x.getGETCount()).average().getAsDouble();
                int avgSet = (int) workerStats.stream().mapToInt(x -> x.getSETCount()).average().getAsDouble();
                int avgMultiget= (int) workerStats.stream().mapToInt(x -> x.getMULTIGETCount()).average().getAsDouble();

                double avgLatency= workerStats.stream().mapToLong(x -> x.getLatency()).average().getAsDouble();

                System.out.println("Average throughput (ops/sec) = " + avgT);
                System.out.println("Average response time (msec) = " + avgLatency / 1000000);
                System.out.println("Average queue length = " + avgLen);
                System.out.println("Average wait time in queue (msec) = " + avgWait / 1000000);
                System.out.println("Average service time (msec) = " + avgServe / 1000000);
                System.out.println("Number of SET = " + avgSet);
                System.out.println("Number of GET = " + avgGet);
                System.out.println("Number of MULTI GET = " + avgMultiget);

//                Worker w = workersPool.stream().filter(x -> workerName.equals(x.getName())).collect(Collectors.toList()).get(0);
//                List<Long> responseTimes = w.getStatistics().getResponseTimesList();
//
//                double avgResponse = responseTimes.stream().mapToLong(time -> time).average().getAsDouble();
//                // nanosec -> mseconds
//                avgResponse /= 1000000;
//                System.out.println("Average response time (msec) = " + Double.toString(avgResponse));

                finalT.add(avgT);
                finalR.add(avgLatency);
                finalLen.add(avgLen);
                finalWait.add(avgWait);
                finalServe.add(avgServe);
                finalGets.add(avgGet);
                finalSets.add(avgSet);
                finalMultiGets.add(avgMultiget);
                System.out.println("\n");
            });
            input.close();


            // TODO: Histogram of repsonse times

            ArrayList<Long> finalResponseTimes = new ArrayList<>();
            //ArrayList<Integer> cacheMisses = new ArrayList<>();
            workersPool.forEach(worker -> {
                List<Long> responseTimes = worker.getStatistics().getResponseTimesList();

                //int count = worker.getStatistics().getCacheMissCount();
                //cacheMisses.add(count);
//                double avgResponse = responseTimes.stream().mapToLong(time -> time).average().getAsDouble();
//                // nanosec -> mseconds
//                avgResponse /= 1000000;
//                System.out.println(worker.getName() + " Average response time (msec) = " + Double.toString(avgResponse));

                //Histogram h = new Histogram(responseTimes);
                //h.printHistogram();
                responseTimes.forEach(x -> finalResponseTimes.add(x));
            });

            System.out.println("FINAL STATS");
            int T = finalT.stream().mapToInt(x -> x).sum();
            double R = finalResponseTimes.stream().mapToLong(x -> x).average().getAsDouble() / 1000000;
            int len = (int) finalLen.stream().mapToInt(x -> x).average().getAsDouble();
            double wait = finalWait.stream().mapToDouble(x -> x).average().getAsDouble() / 1000000;
            double serve = finalServe.stream().mapToDouble(x -> x).average().getAsDouble() / 1000000;
            int gets = (int) finalGets.stream().mapToInt(x -> x).sum();
            int sets = (int) finalSets.stream().mapToInt(x -> x).sum();
            int multiGets= (int) finalMultiGets.stream().mapToInt(x -> x).sum();
            int cacheMissesCount = workersPool.stream().mapToInt(x -> x.getStatistics().getCacheMissCount()).sum();

            double R_window = finalR.stream().mapToDouble(x -> x).average().getAsDouble();


            System.out.println("Average throughput (ops/sec) = " + T);
            System.out.println("Average response time (msec) = " + R);
            System.out.println("Average queue length = " + len);
            System.out.println("Average wait time in queue (msec) = " + wait);
            System.out.println("Average service time (msec) = " + serve);
            System.out.println("Number of SET = " + sets);
            System.out.println("Number of GET = " + gets);
            System.out.println("Number of MULTI GET = " + multiGets);
            System.out.println("Number of Cache misses = " + cacheMissesCount);

            System.out.println("Arrival rate (lambda) = " + Arrays.toString(networkThread.getArrivalRateValues().toArray()));

            System.out.println("\n");

            System.out.println("Histogram response times (0.1 msec step)");
            Histogram h = new Histogram(finalResponseTimes);
            h.printHistogram();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
