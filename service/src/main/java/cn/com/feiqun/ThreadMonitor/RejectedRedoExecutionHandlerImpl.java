package cn.com.feiqun.ThreadMonitor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class RejectedRedoExecutionHandlerImpl implements RejectedExecutionHandler {

    private final AtomicInteger numberOfRejected = new AtomicInteger();

    public AtomicInteger getNumberOfRejected() {
        return numberOfRejected;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        numberOfRejected.incrementAndGet();
        try {
            executor.getQueue().put(r);
        } catch (Exception ex) {
            //todo 这样使用有可能造成不停地执行任务，如果任务一直不成功
        }
    }
}
