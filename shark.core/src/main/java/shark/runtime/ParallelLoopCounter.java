package shark.runtime;

class ParallelLoopCounter {

    private int count = 0;
    private boolean used = false;

    public void increase() {

        synchronized (this) {
            count++;
            used = true;
        }
    }

    public void decrease() {

        synchronized (this) {
            count--;
        }
    }

    public boolean isCompleted() {

        synchronized (this) {
            return used && count == 0;
        }
    }
}