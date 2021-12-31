package carlos.webcrawler;

import java.util.Arrays;

public class ThreadPoolHandler {
    Thread[] threadPool;
    ThreadPoolHandler (int n) {
        threadPool = new Thread[n];
    }

    void setTask(Runnable task) {
        for (int i = 0; i < threadPool.length; i++) {
            threadPool[i] = new Thread(task);
            threadPool[i].start();
        }
    }

    boolean allTerminated() {
        return Arrays.stream(threadPool).noneMatch(Thread::isAlive);
    }
}
