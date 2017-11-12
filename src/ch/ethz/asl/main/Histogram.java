package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class Histogram {

    private static final Logger instrumentationLog = LogManager.getLogger("stat_file");

    private int[] histogram;
    // step in nanosec
    private static final double STEP = 100000;

    public Histogram(List<Long> list) {

        // get msec
        //List<Long> data = list.stream().map(x -> x / 1000000).collect(Collectors.toList());

        long max = list.stream().max(Long::compare).get();
        long min = list.stream().min(Long::compare).get();

        long range = max - min;
        long numBins = (long) (range / STEP);

        histogram = new int[(int) numBins];

        for (double d: list) {
            int bin = (int) ((d - min) / STEP);
            if (bin >= numBins) { /* this data point is bigger than max */ }
            else histogram[bin] += 1;
        }


//        max /= 1000000;
//        min /= 1000000;
//
//
        //System.out.println("SIZE:" + list.size());
        //System.out.println("MAX: " + max);
        //System.out.println("MIN: " + min);
    }

    public void printHistogram() {

        for (int range = 0; range < histogram.length; range++) {
            String label = range * STEP + " : ";
            instrumentationLog.info(label + histogram[range]);
        }
    }

    public void avgHistogram() {
        double avg = 0;
        double sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            avg += (STEP * i) * histogram[i];
            sum += histogram[i];
        }
        avg /= sum;
        System.out.println("AVG from histogram =: " + avg / 1000000);
    }

    private String convertToStars(int num) {
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < num; j++) {
            builder.append('*');
        }
        return builder.toString();
    }

}
