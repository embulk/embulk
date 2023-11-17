package org.embulk.spi.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.msgpack.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class JsonParser {
    public interface Stream extends Closeable {
        Value next() throws IOException;

        void close() throws IOException;
    }

    public JsonParser() {
        if (!hasLogged.getAndSet(true)) {
            logger.warn(
                    "Let the plugin maintainer know: org.embulk.spi.json.JsonParser has been deprecated, but it is used at: ",
                    new Deprecation());
        }
        this.delegate = JsonParserDelegate.of();
    }

    public Stream open(InputStream in) throws IOException {
        return this.delegate.open(in);
    }

    public Stream openWithOffsetInJsonPointer(InputStream in, String offsetInJsonPointer) throws IOException {
        return this.delegate.openWithOffsetInJsonPointer(in, offsetInJsonPointer);
    }

    public Value parse(String json) {
        return this.delegate.parse(json);
    }

    public Value parseWithOffsetInJsonPointer(String json, String offsetInJsonPointer) {
        return this.delegate.parseWithOffsetInJsonPointer(json, offsetInJsonPointer);
    }

    private static class Deprecation extends RuntimeException {
    }

    private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);

    private static final AtomicBoolean hasLogged = new AtomicBoolean(false);

    private final JsonParserDelegate delegate;
}
