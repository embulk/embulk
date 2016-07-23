package org.embulk.spi.json;

import java.io.InputStream;
import java.io.Closeable;
import java.io.IOException;
import org.msgpack.value.Value;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;

@Deprecated
public class JsonParser
{
    public interface Stream
            extends Closeable
    {
        Value next() throws IOException;

        void close() throws IOException;
    }

    private final JsonFactory factory;

    public JsonParser()
    {
        this.factory = new JsonFactory();
        factory.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
    }

    public Stream open(InputStream in) throws IOException
    {
        return new StreamParseContext(factory, in);
    }

    public Value parse(String json)
    {
        return new SingleParseContext(factory, json).parse();
    }

    private static String sampleJsonString(String json)
    {
        if (json.length() < 100) {
            return json;
        }
        else {
            return json.substring(0, 97) + "...";
        }
    }

    private static class StreamParseContext
        extends AbstractParseContext
        implements Stream
    {
        public StreamParseContext(JsonFactory factory, InputStream in)
            throws IOException, JsonParseException
        {
            super(createParser(factory, in));
        }

        private static com.fasterxml.jackson.core.JsonParser createParser(JsonFactory factory, InputStream in)
            throws IOException
        {
            try {
                return factory.createParser(in);
            }
            catch (IOException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new JsonParseException("Failed to parse JSON", ex);
            }
        }

        @Override
        public void close() throws IOException
        {
            parser.close();
        }

        @Override
        protected String sampleJsonString()
        {
            return "in";
        }
    }

    private static class SingleParseContext
        extends AbstractParseContext
    {
        private final String json;

        public SingleParseContext(JsonFactory factory, String json)
        {
            super(createParser(factory, json));
            this.json = json;
        }

        private static com.fasterxml.jackson.core.JsonParser createParser(JsonFactory factory, String json)
        {
            try {
                return factory.createParser(json);
            }
            catch (Exception ex) {
                throw new JsonParseException("Failed to parse JSON: "+JsonParser.sampleJsonString(json), ex);
            }
        }

        public Value parse()
        {
            try {
                Value v = next();
                if (v == null) {
                    throw new JsonParseException("Unable to parse empty string");
                }
                return v;
            }
            catch (IOException ex) {
                throw new JsonParseException("Failed to parse JSON: "+sampleJsonString(), ex);
            }
        }

        @Override
        protected String sampleJsonString()
        {
            return JsonParser.sampleJsonString(json);
        }
    }

    private static abstract class AbstractParseContext
    {
        protected final com.fasterxml.jackson.core.JsonParser parser;

        public AbstractParseContext(com.fasterxml.jackson.core.JsonParser parser)
        {
            this.parser = parser;
        }

        protected abstract String sampleJsonString();

        public Value next() throws IOException
        {
            try {
                return JsonUtil.readJson(parser);
            }
            catch (com.fasterxml.jackson.core.JsonParseException ex) {
                throw new JsonParseException(ex.getMessage() + " at " + ex.getLocation());
            }
        }
    }
}
