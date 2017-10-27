package ch.ethz.asl.main;

// Statistics object for instrumentation
public class Statistics {
    // test interval in seconds
    public final static int testInterval = 2;

    private volatile int jobCount;

    // stats
    private int throughput;
    private volatile int queueLength;

    private long queueWaitTime;
    private long serviceTime;

    private int GETCount;
    private int SETCount;
    private int MULTIGETCount;

    public Statistics() {
        jobCount = 0;

        throughput = 0;
        queueLength = 0;
        queueWaitTime = 0;
        serviceTime = 0;
        GETCount = 0;
        SETCount = 0;
        MULTIGETCount = 0;
    }

    public int getJobCount() {
        return jobCount;
    }

    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public int getThroughput() {

        throughput = jobCount / testInterval;

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
