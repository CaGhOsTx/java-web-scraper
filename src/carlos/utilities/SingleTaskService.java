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
public abstract class SingleTaskService<T> {
    private final List<Thread> threadPool;
    private Runnable task;
    final int initialSize;
    private static int gID = 0;
    private final int ID = ++gID;

    public SingleTaskService() {
        threadPool = new ArrayList<>();
        initialSize = 0;
    }

    public SingleTaskService(int n) {
        threadPool = new ArrayList<>(n);
        initialSize = n;
    }

    /**
     * The task will run using the provided object while this condition is met.
     * @param t derivative object on which to test the condition.
     * @return true if the condition is met.
     */
    public abstract boolean condition(T t);

    /**
     * The {@link SingleTaskService} will compute this action
     * iteratively until the {@link SingleTaskService#condition(Object)} is met,
     * or has not been signalled to stop.
     * @param t derivative object on which to perform the action.
     */
    public abstract void action(T t) throws InterruptedException;

    /**
     * <B>OPTIONAL</B> <br/>
     * Closing action (meant for things like IO operations)
     * @param t derivative object on which to perform the closing action.
     */
    public void close(T t) {}

    /**
     * Stops this {@link SingleTaskService}.<br/>
     */
    final public synchronized void stop(T t) {
        threadPool.forEach(Thread::interrupt);
        while(isRunning()) {
            try {
                this.wait(1_000);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
        close(t);
    }

    /**
     * Starts this {@link SingleTaskService}.
     */
    final public void start(T t) {
        this.task = () -> loop(t);
        allocateThreads(initialSize);
    }

    /**
     * Stops and deallocates n threads.
     * @param n number of threads to be stopped.
     */
    final public synchronized void deallocateThreads(int n) {
        int required = threadPool.size() - n;
        for(int i = 0; i < n; i++)
            threadPool.get(i).interrupt();
        while(threadPool.size() > required) {
            try {
                this.wait(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Allocates and starts n more threads.
     * @param n number of threads to be added.
     */
    final public void allocateThreads(int n) {
        for (int i = threadPool.size(); i < n; i++) {
            threadPool.add(new Thread(task, "STS" + ID + "--Thread-" + i));
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
                if (currentThread().isInterrupted())
                    break;
            }
        }
        catch (Exception e) {
            if(!(e instanceof InterruptedException))
                e.printStackTrace();
        }
        finally {
            synchronized (this) {
                threadPool.remove(currentThread());
                this.notifyAll();
            }
        }
    }

    @Override
    public String toString() {
        return "SingleTaskService{" +
                "threadPool=" + threadPool +
                '}';
    }
}
