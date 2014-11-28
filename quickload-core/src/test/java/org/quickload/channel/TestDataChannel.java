package org.quickload.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.quickload.DurationWatch;
import org.quickload.buffer.Buffer;

public class TestDataChannel
{
    private DataChannel<Buffer> channel;

    // to make sure methods won't be blocked for ever
    @Rule
    public Timeout globalTimeout = new Timeout(10000);

    @Rule
    public DurationWatch duration = new DurationWatch();

    @Before
    public void setup() throws Exception
    {
        channel = new DataChannel<Buffer>(Integer.MAX_VALUE);
    }

    @After
    public void destroy() throws Exception
    {
    }

    private static class MockListener
            implements DataChannel.Listener<Buffer>
    {
        private List<Buffer> elements = new ArrayList<Buffer>();

        @Override
        public void add(Buffer e)
        {
            elements.add(e);
        }
    }

    static Buffer newBuffer()
    {
        return newBuffer(10);
    }

    static Buffer newBuffer(int size)
    {
        return Buffer.allocate(size);
    }

    static Buffer newFilledBuffer(int size)
    {
        Buffer b = Buffer.allocate(size);
        b.limit(size);
        return b;
    }

    static void doLater(final Runnable op)
    {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("interrupted", ex);
                }
                op.run();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Test
    public void testOrderedPoll() throws Exception
    {
        Buffer b1 = newBuffer();
        Buffer b2 = newBuffer();
        channel.add(b1);
        channel.add(b2);
        assertTrue(channel.poll() == b1);
        assertTrue(channel.poll() == b2);
    }

    @Test
    public void testIterator() throws Exception
    {
        Buffer b1 = newBuffer();
        Buffer b2 = newBuffer();
        channel.add(b1);
        channel.add(b2);
        channel.completeProducer();
        Iterator<Buffer> ite = channel.iterator();
        assertTrue(ite.hasNext());
        assertTrue(ite.next() == b1);
        assertTrue(ite.next() == b2);
        assertFalse(ite.hasNext());
    }
    
    @Test(expected = NoSuchElementException.class)
    public void testNextThrowsWhenItDoesNotHave() throws Exception
    {
        channel.add(newBuffer());
        channel.completeProducer();
        Iterator<Buffer> ite = channel.iterator();
        assertTrue(ite.hasNext());
        ite.next();
        assertFalse(ite.hasNext());
        ite.next();
    }

    @Test
    public void testIteratorHasNextBlocksUntilAdd() throws Exception
    {
        Iterator<Buffer> ite = channel.iterator();
        final Buffer b1 = newBuffer();
        doLater(new Runnable() {
            public void run()
            {
                channel.add(b1);
            }
        });
        assertTrue(ite.hasNext());
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test
    public void testIteratorNextBlocksUntilAdd() throws Exception
    {
        Iterator<Buffer> ite = channel.iterator();
        final Buffer b1 = newBuffer();
        doLater(new Runnable() {
            public void run()
            {
                channel.add(b1);
            }
        });
        assertTrue(ite.next() == b1);
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveIsUnsupported() throws Exception
    {
        channel.iterator().remove();
    }

    @Test
    public void testPollBlocksUntilAdd() throws Exception
    {
        final Buffer b1 = newBuffer();
        doLater(new Runnable() {
            public void run()
            {
                channel.add(b1);
            }
        });
        assertTrue(channel.poll() == b1);
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test
    public void testCloseReleasesUnconsumedBuffers()
    {
        channel.add(newBuffer());
        channel.close();
    }

    @Test
    public void testBlockedAddThrowsAfterConsumerCompleted() throws Exception
    {
        channel = new DataChannel<Buffer>(1);
        channel.add(newBuffer(1));
        doLater(new Runnable() {
            public void run()
            {
                channel.completeConsumer();
            }
        });
        try {
            // blocks, and throws after released by close()
            channel.add(newBuffer(1));
            fail();
        } catch (ChannelAsynchronousCloseException cace) {
            // Make sure code comes here.
        }
    }

    @Test
    public void testBlockedAddThrowsAfterProducerCompleted() throws Exception
    {
        channel = new DataChannel<Buffer>(1);
        channel.add(newBuffer(1));
        doLater(new Runnable() {
            public void run()
            {
                channel.completeProducer();
            }
        });
        try {
            // blocks, and throws after released by close()
            channel.add(newBuffer(1));
            fail();
        } catch (IllegalStateException ise) {
            // Make sure code comes here.
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullFails() throws Exception
    {
        channel.add(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddFailsAfterCompleteProducer() throws Exception
    {
        channel.completeProducer();
        channel.add(newBuffer());
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testAddFailsAfterCompleteConsumer() throws Exception
    {
        channel.completeConsumer();
        channel.add(newBuffer());
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testAddFailsAfterClose() throws Exception
    {
        channel.close();
        channel.add(newBuffer());
    }

    @Test
    public void testPollReturnsNullAfterCompleteProducer() throws Exception
    {
        channel.completeProducer();
        assertNull(channel.poll());
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testPollFailsAfterCompleteConsumer() throws Exception
    {
        channel.completeConsumer();
        channel.poll();
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testPollFailsAfterClose() throws Exception
    {
        channel.close();
        channel.poll();
    }

    @Test
    public void testMaxQueuedSizeBlocksConsumer() throws Exception
    {
        channel = new DataChannel<Buffer>(10);
        channel.add(newBuffer(5));
        channel.add(newBuffer(5));
        doLater(new Runnable() {
            public void run()
            {
                channel.poll();
            }
        });
        channel.add(newBuffer(5));
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test
    public void testSetListenerReceivesNewElements() throws Exception
    {
        MockListener listener = new MockListener();
        channel.setListener(listener);
        Buffer b1 = newBuffer();
        Buffer b2 = newBuffer();
        channel.add(b1);
        channel.add(b2);
        assertEquals(Arrays.asList(b1, b2), listener.elements);
    }

    @Test
    public void testSetListenerConsumesExistentElements() throws Exception
    {
        Buffer b1 = newBuffer();
        Buffer b2 = newBuffer();
        channel.add(b1);
        channel.add(b2);
        MockListener listener = new MockListener();
        channel.setListener(listener);
        assertEquals(Arrays.asList(b1, b2), listener.elements);
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testSetListenerFailsAfterCompleteConsumer() throws Exception
    {
        channel.completeConsumer();
        channel.setListener(new MockListener());
    }

    @Test
    public void testJoinBlocksUntilCompleteProducerAndConsumerClosed() throws Exception
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.completeProducer();
                channel.completeConsumer();
            }
        });
        channel.join();
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testCompleteConsumerBeforeIncompleteConsumptionMakesJoinFailed() throws Exception
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.add(newBuffer());
                channel.completeProducer();
                channel.completeConsumer();
            }
        });
        channel.join();
    }

    // TODO which is expected behavior?
    //@Test(expected = ChannelAsynchronousCloseException.class)
    //public void testCloseBeforeIncompleteConsumptionMakesJoinFailed() throws Exception
    //{
    //    doLater(new Runnable() {
    //        public void run()
    //        {
    //            channel.add(newBuffer());
    //            channel.completeProducer();
    //            channel.close();
    //        }
    //    });
    //    channel.join();
    //}

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testAsyncCloseMakesBlockedJoinFailed() throws Exception
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.close();
            }
        });
        channel.join();
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testAsyncCompleteConsumerMakesBlockedJoinFailed() throws Exception
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.completeConsumer();
            }
        });
        channel.join();
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testAsyncCloseMakesBlockedPollFailed() throws Exception
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.close();
            }
        });
        channel.join();
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testAsyncCompleteConsumerMakesBlockedPollFailed() throws Exception
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.completeConsumer();
            }
        });
        channel.poll();
        assertTrue(duration.runtimeMillis() > 50);
    }
}
