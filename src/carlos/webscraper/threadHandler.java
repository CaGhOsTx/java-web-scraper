package carlos.webscraper;

import static java.util.Arrays.stream;

final class threadHandler {
    private final Thread[] threadPool;
    private boolean stop = false;
    private Runnable task;
    threadHandler(int n) {
        threadPool = new Thread[n];
    }

    threadHandler setTask(Runnable startTask) {
        this.task = startTask;
        return this;
    }

    void stop() {
        stop = true;
    }

    private boolean hasATask() {
        return task != null;
    }

    synchronized void start() {
        if(!hasATask()) throw new IllegalStateException("Tasks are not set");
        stop = false;
        for (int i = 0; i < threadPool.length; i++) {
            threadPool[i] = new Thread(task);
            threadPool[i].start();
        }
    }

    synchronized boolean allTerminated() {
        return hasATask() && stream(threadPool).noneMatch(Thread::isAlive);
    }

    int size() {
        return threadPool.length;
    }

    boolean shouldStop() {
        return stop;
    }

    synchronized boolean isLastThread() {
        return stream(threadPool).filter(Thread::isAlive).count() == 1;
    }

    synchronized void notifyScraper(WebScraper scraper) {
        scraper.notifyAll();
    }
}
