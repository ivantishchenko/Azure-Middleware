package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

// Aggregates the results from Log file
public class ShutDownHook extends Thread{

    private static final Logger log = LogManager.getLogger(ShutDownHook.class);

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

                Statistics s = new Statistics(name, t, len, wait, serve, get, set, multiget, null);
                return s;
            }).collect(Collectors.toList());

            Map<String, List<Statistics>> statsByWorker = statsList.stream().collect(groupingBy(Statistics::getWorkerName));

            statsByWorker.forEach((worker, workerStats) -> {
                System.out.println(worker);

                int avgT = (int) workerStats.stream().mapToInt(x -> x.getThroughput()).average().getAsDouble();
                int avgLen = (int) workerStats.stream().mapToInt(x -> x.getQueueLength()).average().getAsDouble();
                int avgWait = (int) workerStats.stream().mapToLong(x -> x.getQueueWaitTime()).average().getAsDouble();
                int avgServe = (int) workerStats.stream().mapToLong(x -> x.getServiceTime()).average().getAsDouble();
                int avgGet = (int) workerStats.stream().mapToInt(x -> x.getGETCount()).average().getAsDouble();
                int avgSet = (int) workerStats.stream().mapToInt(x -> x.getSETCount()).average().getAsDouble();
                int avgMultiget= (int) workerStats.stream().mapToInt(x -> x.getMULTIGETCount()).average().getAsDouble();

                System.out.println("Average throughput = " + avgT);
                System.out.println("Average queue length = " + avgLen);
                System.out.println("Average wait time in queue = " + avgWait);
                System.out.println("Average service time = " + avgServe);
                System.out.println("Number of SET = " + avgSet);
                System.out.println("Number of GET = " + avgGet);
                System.out.println("Number of MULTI GET = " + avgMultiget);
                System.out.println("\n");

            });

            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
