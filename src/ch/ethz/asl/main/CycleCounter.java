package ch.ethz.asl.main;

// Atomic modular counter for the Round Robin Pattern
public class CycleCounter {

    private final int max;
    private int count;

    public CycleCounter(int max) {
        if (max < 1) {
            throw new IllegalArgumentException();
        }

        this.max = max;
    }

    public synchronized int getCount() {
        return count;
    }

    public synchronized int increment() {
        count = (count + 1) % max;
        return count;
    }
}
