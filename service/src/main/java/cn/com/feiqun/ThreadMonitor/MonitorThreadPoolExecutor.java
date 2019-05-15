package cn.com.feiqun.ThreadMonitor;

import java.util.LinkedHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MonitorThreadPoolExecutor extends ThreadPoolExecutor {

    private int queueCapacity;
    private String poolName;
    private boolean redo;

    //当前任务开始执行时间
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    //已完成的任务数量
    private final AtomicInteger numberOfRequestsRetired = new AtomicInteger();

    //成功任务数量
    private final AtomicInteger numberOfSuccess = new AtomicInteger();
    //异常任务数量
    private final AtomicInteger numberOfExceptions = new AtomicInteger();
    //超时任务数量
    private final AtomicInteger numberOfTimeouts = new AtomicInteger();
    //任务自身执行总时间
    private final AtomicLong totalServiceTime = new AtomicLong();
    //任务最大执行时间
    private final AtomicLong maxServiceTime = new AtomicLong();

    //任务在线程池执行前等候总时间
    private final AtomicLong totalThreadWaitTime = new AtomicLong();

    public MonitorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, int queueCapacity, final String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new MonitorLinkedBlockingQueue<>(queueCapacity), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                final String threadName = String.format("%s-%d", poolName, counter.incrementAndGet());
                return new Thread(r, threadName);
            }
        });
        this.queueCapacity = queueCapacity;
        this.poolName = poolName;
    }

    public MonitorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, int queueCapacity, final String poolName, boolean redo) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new MonitorLinkedBlockingQueue<>(queueCapacity), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                final String threadName = String.format("%s-%d", poolName, counter.incrementAndGet());
                return new Thread(r, threadName);
            }
        }, new RejectedRedoExecutionHandlerImpl());
        this.queueCapacity = queueCapacity;
        this.poolName = poolName;
        this.redo = redo;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        //任务开始执行时间
        startTime.set(Long.valueOf(System.nanoTime()));
    }

    @Override
    public void execute(Runnable command) {
        //当前时间-任务开始时间=等候时间
        totalThreadWaitTime.addAndGet(System.nanoTime() - startTime.get().longValue());
        super.execute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            long start = startTime.get().longValue();
            //当前时间-任务开始执行时间=任务执行时间
            long serviceTime = System.nanoTime() - start;
            totalServiceTime.addAndGet(serviceTime);
            if (maxServiceTime.get() < serviceTime) {
                maxServiceTime.set(serviceTime);
            }
            //已完成的任务数量
            numberOfRequestsRetired.incrementAndGet();
        } finally {
            super.afterExecute(r, t);
        }
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                t = ce;
            } catch (InterruptedException e) {
                t = e.getCause();
            } catch (ExecutionException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            if (t.toString().contains("CancellationException")) {
                //超时异常取消任务
                numberOfTimeouts.incrementAndGet();
            }
            numberOfExceptions.incrementAndGet();
        } else {
            //执行成功任务数量
            numberOfSuccess.incrementAndGet();
        }
    }

    public String getMonitorResult(boolean pretty) {
        LinkedHashMap basicInfo = new LinkedHashMap();
        int activeCount = this.getActiveCount();
        int corePoolSize = this.getCorePoolSize();
        basicInfo.put("名称", this.poolName);
        basicInfo.put("核心线程数", corePoolSize);
        basicInfo.put("最大线程数", getMaximumPoolSize());
        basicInfo.put("激活线程数", activeCount);
        basicInfo.put("最大激活线程数", getLargestPoolSize());
        if (redo) {
            basicInfo.put("拒绝任务重新处理", ((RejectedRedoExecutionHandlerImpl) getRejectedExecutionHandler()).getNumberOfRejected());
        }
        LinkedHashMap timeInfo = new LinkedHashMap();
        int numberOfRequestsRetiredValue = numberOfRequestsRetired.intValue();
        timeInfo.put("平均执行时间", totalServiceTime.longValue() / numberOfRequestsRetiredValue);
        timeInfo.put("平均等待时间", totalServiceTime.longValue() / numberOfRequestsRetiredValue);
        timeInfo.put("任务最大执行时间", maxServiceTime);

        LinkedHashMap queueInfo = new LinkedHashMap();
        queueInfo.put("当前数量", getQueue().size());
        queueInfo.put("历史极值", ((MonitorLinkedBlockingQueue) this.getQueue()).getLargestQueueSize());
        queueInfo.put("最大容量", queueCapacity);
        queueInfo.put("队列统计", queueInfo);
        return "";//todo
    }
}