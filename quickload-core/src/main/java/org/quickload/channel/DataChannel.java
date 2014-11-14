package org.quickload.channel;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import org.quickload.buffer.Buffer;

public class DataChannel <E extends Buffer>
        implements AutoCloseable, Iterable<E>
{
    public static interface Listener <E>
    {
        public void add(E e);
    }

    private static class Ite <E extends Buffer>
            implements Iterator<E>
    {
        private DataChannel<E> channel;
        private E element;

        public Ite(DataChannel<E> channel)
        {
            this.channel = channel;
        }

        @Override
        public boolean hasNext()
        {
            if (element != null) {
                return true;
            } else {
                element = channel.poll();
                return element != null;
            }
        }

        @Override
        public E next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E e = element;
            element = null;
            return e;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    private final int maxQueuedSize;

    private final Queue<E> queue = new LinkedList<E>();
    private final ReentrantLock sync = new ReentrantLock();
    private final Condition queueCondition = sync.newCondition();
    private final Condition joinProducerCondition = sync.newCondition();

    private volatile int queuedSize;
    private volatile Listener<E> listener;
    private volatile boolean producerCompleted;
    private volatile boolean closed;

    public DataChannel(int maxQueuedSize)
    {
        this.maxQueuedSize = maxQueuedSize;
    }

    public void setListener(Listener<E> op)
    {
        sync.lock();
        try {
            if (closed) {
                throw new ChannelAsynchronousCloseException("Channel consumer already closed");
            }

            // ensure queue is empty because add() and complete() directly call consumer
            while (!queue.isEmpty()) {
                op.add(poll());
            }

            listener = op;
            queueCondition.signalAll();

        } finally {
            sync.unlock();
        }
    }

    public void completeProducer()
    {
        sync.lock();
        try {
            this.producerCompleted = true;
            queueCondition.signalAll();
            joinProducerCondition.signalAll();
        } finally {
            sync.unlock();
        }
    }

    public void completeConsumer()
    {
        sync.lock();
        try {
            this.closed = true;
            queueCondition.signalAll();
            joinProducerCondition.signalAll();
        } finally {
            sync.unlock();
        }
    }

    public void join()
    {
        sync.lock();
        try {
            while (!producerCompleted) {
                if (closed) {
                    throw new ChannelAsynchronousCloseException("Channel consumer already closed");
                }
                try {
                    joinProducerCondition.await();
                } catch (InterruptedException ex) {
                    throw new ChannelInterruptedException(ex);
                }
            }

            while (!closed) {
                try {
                    queueCondition.await();
                } catch (InterruptedException ex) {
                    throw new ChannelInterruptedException(ex);
                }
            }

            if (!queue.isEmpty()) {
                throw new ChannelAsynchronousCloseException("Channel consumer closed afer consuming all data");
            }

        } finally {
            sync.unlock();
        }
    }

    // TODO batch poll
    public E poll()
    {
        sync.lock();
        try {
            if (closed) {
                if (producerCompleted && queue.isEmpty()) {
                    return null;
                }
                throw new ChannelAsynchronousCloseException("Channel already closed");
            }

            E e = queue.poll();
            while (e == null) {
                if (producerCompleted) {
                    return null;
                }
                try {
                    queueCondition.await();
                } catch (InterruptedException ex) {
                    throw new ChannelInterruptedException(ex);
                }
                if (closed) {
                    if (producerCompleted && queue.isEmpty()) {
                        return null;
                    }
                    throw new ChannelAsynchronousCloseException("Channel already closed");
                }
                e = queue.poll();
            }

            queuedSize -= e.capacity();
            queueCondition.signalAll();
            return e;

        } finally {
            sync.unlock();
        }
    }

    public Iterator<E> iterator()
    {
        return new Ite(this);
    }

    public void add(E e)
    {
        if (e == null) {
            throw new NullPointerException();
        }

        sync.lock();
        try {
            if (closed) {
                throw new ChannelAsynchronousCloseException("Channel consumer already completed");
            }
            if (producerCompleted) {
                throw new IllegalStateException("add() called after completeProducer()");
            }

            if (listener != null) {
                listener.add(e);
                e = null;
            } else {
                int nextQueuedSize = queuedSize + e.capacity();
                while (nextQueuedSize > maxQueuedSize) {
                    try {
                        queueCondition.await();
                    } catch (InterruptedException ex) {
                        throw new ChannelInterruptedException(ex);
                    }
                    nextQueuedSize = queuedSize + e.capacity();
                    if (closed) {
                        throw new ChannelAsynchronousCloseException("Channel consumer already completed");
                    }
                    if (producerCompleted) {
                        throw new IllegalStateException("add() called after completeProducer()");
                    }
                }
                queue.offer(e);
                e = null;
                queuedSize = nextQueuedSize;
                queueCondition.signalAll();
            }
        } finally {
            sync.unlock();
            if (e != null) {
                e.release();
            }
        }
    }

    @Override
    public void close()
    {
        sync.lock();
        try {
            this.closed = true;
            queueCondition.signalAll();
            joinProducerCondition.signalAll();

            // clear queue
            E e;
            while ((e = queue.poll()) != null) {
                e.release();
            }
            queuedSize = 0;

        } finally {
            sync.unlock();
        }
    }
}
