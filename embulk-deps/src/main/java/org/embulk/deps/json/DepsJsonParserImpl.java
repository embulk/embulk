package org.embulk.deps.json;

import java.io.IOException;
import java.io.InputStream;
import org.msgpack.value.Value;

public class DepsJsonParserImpl extends org.embulk.deps.json.DepsJsonParser {
    public DepsJsonParserImpl() {
        this.delegate = new org.embulk.util.json.JsonParser();
    }

    private class CoreStreamFromUtilStream implements org.embulk.spi.json.JsonParser.Stream {
        CoreStreamFromUtilStream(final org.embulk.util.json.JsonParser.Stream utilStream) {
            this.utilStream = utilStream;
        }

        @Override
        public Value next() throws IOException {
            return this.utilStream.next();
        }

        @Override
        public void close() throws IOException {
            this.utilStream.close();
        }

        private final org.embulk.util.json.JsonParser.Stream utilStream;
    }

    @Override
    public org.embulk.spi.json.JsonParser.Stream open(final InputStream in) throws IOException {
        try {
            return this.openWithOffsetInJsonPointer(in, null);
        } catch (final org.embulk.util.json.JsonParseException ex) {
            throw new org.embulk.spi.json.JsonParseException(ex.getMessage(), ex);
        }
    }

    @Override
    public org.embulk.spi.json.JsonParser.Stream openWithOffsetInJsonPointer(
            final InputStream in, final String offsetInJsonPointer) throws IOException {
        try {
            return new CoreStreamFromUtilStream(this.delegate.openWithOffsetInJsonPointer(in, offsetInJsonPointer));
        } catch (final org.embulk.util.json.JsonParseException ex) {
            throw new org.embulk.spi.json.JsonParseException(ex.getMessage(), ex);
        }
    }

    @Override
    public Value parse(final String json) {
        try {
            return this.parseWithOffsetInJsonPointer(json, null);
        } catch (final org.embulk.util.json.JsonParseException ex) {
            throw new org.embulk.spi.json.JsonParseException(ex.getMessage(), ex);
        }
    }

    @Override
    public Value parseWithOffsetInJsonPointer(final String json, final String offsetInJsonPointer) {
        try {
            return this.delegate.parseWithOffsetInJsonPointer(json, offsetInJsonPointer);
        } catch (final org.embulk.util.json.JsonParseException ex) {
            throw new org.embulk.spi.json.JsonParseException(ex.getMessage(), ex);
        }
    }

    private final org.embulk.util.json.JsonParser delegate;
}
