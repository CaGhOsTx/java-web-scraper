package carlos.webcrawler;
import java.util.Arrays;

public class ThreadPoolHandler {
    Thread[] threadPool;
    boolean stop = false;
    Runnable task;
    ThreadPoolHandler (int n) {
        threadPool = new Thread[n];
    }

    ThreadPoolHandler setTask(Runnable task) {
        this.task = task;
        return this;
    }

    synchronized void close() {
        stop = true;
        while(!allTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void start() {
        stop = false;
        for (int i = 0; i < threadPool.length; i++) {
            threadPool[i] = new Thread(task);
            threadPool[i].start();
        }
    }

    boolean allTerminated() {
        return Arrays.stream(threadPool).noneMatch(Thread::isAlive);
    }

    synchronized int size() {
        return threadPool.length;
    }

    public boolean shouldStop(Thread t) {
        return stop;
    }
}
