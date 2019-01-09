package org.embulk.deps;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;

/**
 * A special type of {@link java.net.URLStreamHandler} to be coupled with URLs created for a resource.
 */
final class JarEmbeddedUrlStreamHandler extends URLStreamHandler {
    JarEmbeddedUrlStreamHandler(final ByteBuffer buffer, final int begin, final int end) {
        this.buffer = buffer;
        this.begin = begin;
        this.end = end;
    }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        // Note that declaring variables here may cause unexpected behaviors.
        //
        // @see <a href="https://stackoverflow.com/questions/9952815/s3-java-client-fails-a-lot-with-premature-end-of-content-length-delimited-messa">S3 Java client fails a lot with "Premature end of Content-Length delimited message body" or "java.net.SocketException Socket closed" - Stack Overflow</a>
        // @see <a href="https://forums.aws.amazon.com/thread.jspa?threadID=83326">Android s3 download throws "Socket is closed" exception or terminates early</a>
        return new URLConnection(url) {
            @Override
            public void connect() {
                // Do nothing.
            }

            /**
             * Breaks down the special URL built by JarEmbeddedUrlFactory, and creates InputStream to read from.
             */
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteBufferInputStream(buffer, begin, end);
            }
        };
    }

    private final ByteBuffer buffer;
    private final int begin;
    private final int end;
}
