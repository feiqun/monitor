package cn.com.feiqun.ThreadMonitor;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitorLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private AtomicInteger largestQueueSize = new AtomicInteger();

    public MonitorLinkedBlockingQueue(int capacity) {
        super(capacity);
    }

    public AtomicInteger getLargestQueueSize() {
        return largestQueueSize;
    }

    @Override
    public boolean add(E o) {
        if (super.add(o)) {
            updateLargestQueueSize();
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection c) {
        if (super.addAll(c)) {
            updateLargestQueueSize();
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        if (super.offer(o, timeout, unit)) {
            updateLargestQueueSize();
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(E o) {
        if (super.offer(o)) {
            updateLargestQueueSize();
            return true;
        }
        return false;
    }

    @Override
    public void put(E o) throws InterruptedException {
        super.put(o);
        updateLargestQueueSize();
    }

    private void updateLargestQueueSize() {
        final int size = size();
        if (size > largestQueueSize.intValue()) {
            largestQueueSize.set(size);
        }
    }
}
