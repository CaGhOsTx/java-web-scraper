package carlos.utilities;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.currentThread;

/**
 * Class which implements concurrency for parallel computing
 * of a repetitive task.
 * @author Carlos Milkovic
 * @version 1.1
 */
public abstract class RepetitiveTaskService<T> {
    private final List<Thread> threadPool;
    private volatile boolean stop = true;
    private Runnable task;
    final int initialSize;

    public RepetitiveTaskService() {
        threadPool = new ArrayList<>();
        initialSize = 0;
    }

    public RepetitiveTaskService(int n) {
        threadPool = new ArrayList<>(n);
        initialSize = n;
    }

    /**
     * Submits the following Object to this {@link RepetitiveTaskService}.
     * @param t derivative object on which to compute the implemented task.
     * @return this {@link RepetitiveTaskService} instance.
     */
    public RepetitiveTaskService<T> submit(T t) {
        this.task = () -> loop(t);
        return this;
    }

    /**
     * The task will run using the provided object while this condition is met.
     * @param t derivative object on which to test the condition.
     * @return true if the condition is met.
     */
    public abstract boolean condition(T t);

    /**
     * The {@link RepetitiveTaskService} will compute this action
     * iteratively until the {@link RepetitiveTaskService#condition(Object)} is met,
     * or has not been signalled to stop.
     * @param t derivative object on which to perform the action.
     */
    public abstract void action(T t);

    /**
     * <B>OPTIONAL</B> <br/>
     * Closing action (meant for things like IO operations)
     * @param t derivative object on which to perform the closing action.
     */
    public void close(T t) {}

    /**
     * Stops this {@link RepetitiveTaskService}.<br/>
     */
    final public synchronized void stop() {
        stop = true;
        while(isRunning()) {
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stop = false;
    }

    /**
     * Starts this {@link RepetitiveTaskService}.
     */
    final public void start() {
        stop = false;
        allocateThreads(initialSize);
    }

    /**
     * Stops and deallocates n threads.
     * @param n number of threads to be stopped.
     */
    final public synchronized void deallocateThreads(int n) {
        int required = threadPool.size() - n;
        while(threadPool.size() > required) {
            stop = true;
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stop = false;
    }

    /**
     * Allocates and starts n more threads.
     * @param n number of threads to be added.
     */
    final public void allocateThreads(int n) {
        for (int i = threadPool.size(); i < n; i++) {
            threadPool.add(new Thread(task));
            threadPool.get(i).setDaemon(true);
            threadPool.get(i).start();
        }
    }

    final public boolean isRunning() {
        return !threadPool.isEmpty();
    }

    /**
     * @return Amount of {@link Thread threads} alive.
     */
    final public int size() {
        return threadPool.size();
    }

     private void loop(T t) {
        try {
            while (condition(t)) {
                action(t);
                if (shouldStop())
                    break;
            }
        }
        catch (Exception e) {
            System.err.println("CRASH: " + e.getMessage());
            threadPool.remove(currentThread());
        }
        if (isLastThread()) {
            close(t);
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * @return true if the entrant thread should stop.
     */
    private boolean shouldStop() {
        if(stop)
            threadPool.remove(currentThread());
        return stop;
    }

    final public synchronized boolean isLastThread() {
        return threadPool.isEmpty();
    }
}