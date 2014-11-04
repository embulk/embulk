package org.quickload.channel;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import org.quickload.buffer.Buffer;

public class TestDataChannel
{
    private DataChannel<Buffer> channel = new DataChannel<Buffer>(100);

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

    @Before
    public void setup() throws Exception
    {
    }

    @After
    public void destroy() throws Exception
    {
    }

    private static Buffer newBuffer()
    {
        return newBuffer(10);
    }

    private static Buffer newBuffer(int size)
    {
        return new Buffer(ByteBuffer.allocate(size));
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
        // TODO
    }

    @Test
    public void testSetListenerReceivesNewElements() throws Exception
    {
        // TODO
    }

    @Test
    public void testSetListenerConsumesExistentElements() throws Exception
    {
        // TODO
    }

    @Test(expected = ChannelAsynchronousCloseException.class)
    public void testSetListenerFailsAfterCompleteConsumer() throws Exception
    {
        channel.completeConsumer();
        channel.setListener(new MockListener());
    }

    @Test
    public void testIterator() throws Exception
    {
        // TODO
    }

    @Test
    public void testJoinBlocksUntilCompleteProducerAndConsumerClosed() throws Exception
    {
        // TODO
    }

    @Test
    public void testJoinBlocksUntilClose() throws Exception
    {
        // TODO
    }

    @Test
    public void testCloseMakesBlockedJoinFailed() throws Exception
    {
        // TODO
    }

    @Test
    public void testCompleteConsumerMakesBlockedJoinFailed() throws Exception
    {
        // TODO
    }

    @Test
    public void testCloseMakesBlockedPollFailed() throws Exception
    {
        // TODO
    }

    @Test
    public void testCompleteConsumerMakesBlockedPollFailed() throws Exception
    {
        // TODO
    }
}
