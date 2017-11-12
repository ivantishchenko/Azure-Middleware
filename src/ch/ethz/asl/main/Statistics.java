package ch.ethz.asl.main;

import java.util.ArrayList;
import java.util.List;

// Statistics object for instrumentation
public class Statistics {
    // test interval in seconds
    public final static int testInterval = 1;

    private volatile int jobCount;

    // stats
    private int throughput;
    private volatile int queueLength;

    private long queueWaitTime;
    private long serviceTime;

    private int GETCount;
    private int SETCount;
    private int MULTIGETCount;

    private String workerName;
    private List<Long> responseTimesList;

    private long latency;

    private int cacheMissCount;

    public Statistics(String name, int throughput, int queueLength, long queueWaitTime, long serviceTime, int SETCount, int GETCount, int MULTIGETCount, long latency) {
        this.workerName = name;
        this.throughput = throughput;
        this.queueLength = queueLength;
        this.queueWaitTime = queueWaitTime;
        this.serviceTime = serviceTime;
        this.SETCount = SETCount;
        this.GETCount = GETCount;
        this.MULTIGETCount = MULTIGETCount;
        this.latency = latency;
        this.cacheMissCount = 0;
    }

    public Statistics(String name, int throughput, int queueLength, long queueWaitTime, long serviceTime, int SETCount, int GETCount, int MULTIGETCount, List<Long> times) {
        this.workerName = name;
        this.throughput = throughput;
        this.queueLength = queueLength;
        this.queueWaitTime = queueWaitTime;
        this.serviceTime = serviceTime;
        this.SETCount = SETCount;
        this.GETCount = GETCount;
        this.MULTIGETCount = MULTIGETCount;
        this.responseTimesList = times;
        this.cacheMissCount = 0;
    }

    public Statistics() {
        jobCount = 0;
        throughput = 0;
        queueLength = 0;
        queueWaitTime = 0;
        serviceTime = 0;
        GETCount = 0;
        SETCount = 0;
        MULTIGETCount = 0;
        latency = 0;
        responseTimesList = new ArrayList<>();
        cacheMissCount = 0;
    }

    public int getCacheMissCount() {
        return cacheMissCount;
    }

    public void setCacheMissCount(int cacheMissCount) {
        this.cacheMissCount = cacheMissCount;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }


    public void addResponseTime(Long val) {
        responseTimesList.add(val);
    }

    public List<Long> getResponseTimesList() {
        return responseTimesList;
    }

    public void setResponseTimesList(List<Long> responseTimesList) {
        this.responseTimesList = responseTimesList;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public int getJobCount() {
        return jobCount;
    }

    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public int getThroughputOverInterval() {

        throughput = jobCount / testInterval;

        return throughput;
    }

    public int getThroughput() {
        return throughput;
    }

    public void setThroughput(int throughput) {
        this.throughput = throughput;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }

    public long getQueueWaitTime() {
        return queueWaitTime;
    }

    public void setQueueWaitTime(long queueWaitTime) {
        this.queueWaitTime = queueWaitTime;
    }

    public long getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(long serviceTime) {
        this.serviceTime = serviceTime;
    }

    public int getGETCount() {
        return GETCount;
    }

    public void setGETCount(int GETCount) {
        this.GETCount = GETCount;
    }

    public int getSETCount() {
        return SETCount;
    }

    public void setSETCount(int SETCount) {
        this.SETCount = SETCount;
    }

    public int getMULTIGETCount() {
        return MULTIGETCount;
    }

    public void setMULTIGETCount(int MULTIGETCount) {
        this.MULTIGETCount = MULTIGETCount;
    }

}
