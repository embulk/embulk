package org.embulk.spi.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import org.embulk.deps.json.DepsJsonParser;
import org.msgpack.value.Value;

@Deprecated  // Externalized to embulk-util-json
public class JsonParser {
    public interface Stream extends Closeable {
        Value next() throws IOException;

        void close() throws IOException;
    }

    public JsonParser() {
        this.delegate = DepsJsonParser.of();
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

    private final DepsJsonParser delegate;
}
