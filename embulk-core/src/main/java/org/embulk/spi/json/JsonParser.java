package org.embulk.spi.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

public class JsonParser {
    public interface Stream extends Closeable {
        Value next() throws IOException;

        void close() throws IOException;
    }

    private final JsonFactory factory;

    public JsonParser() {
        this.factory = new JsonFactory();
        factory.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        factory.enable(Feature.ALLOW_NON_NUMERIC_NUMBERS);
    }

    public Stream open(InputStream in) throws IOException {
        return openWithOffsetInJsonPointer(in, null);
    }

    public Stream openWithOffsetInJsonPointer(InputStream in, String offsetInJsonPointer) throws IOException {
        return new StreamParseContext(factory, in, offsetInJsonPointer);
    }

    public Value parse(String json) {
        return parseWithOffsetInJsonPointer(json, null);
    }

    public Value parseWithOffsetInJsonPointer(String json, String offsetInJsonPointer) {
        return new SingleParseContext(factory, json, offsetInJsonPointer).parse();
    }

    private static String sampleJsonString(String json) {
        if (json.length() < 100) {
            return json;
        } else {
            return json.substring(0, 97) + "...";
        }
    }

    private static com.fasterxml.jackson.core.JsonParser wrapWithPointerFilter(
            com.fasterxml.jackson.core.JsonParser baseParser, String offsetInJsonPointer) {
        return new FilteringParserDelegate(baseParser, new JsonPointerBasedFilter(offsetInJsonPointer), false, false);
    }

    private static class StreamParseContext extends AbstractParseContext implements Stream {
        public StreamParseContext(JsonFactory factory, InputStream in, String offsetInJsonPointer) throws IOException {
            super(createParser(factory, in, Optional.ofNullable(offsetInJsonPointer)));
        }

        private static com.fasterxml.jackson.core.JsonParser createParser(
                JsonFactory factory, InputStream in, Optional<String> offsetInJsonPointer) throws IOException {
            try {
                final com.fasterxml.jackson.core.JsonParser baseParser = factory.createParser(in);
                return offsetInJsonPointer.map(p -> wrapWithPointerFilter(baseParser, p)).orElse(baseParser);
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new JsonParseException("Failed to parse JSON", ex);
            }
        }

        @Override
        public void close() throws IOException {
            parser.close();
        }

        @Override
        protected String sampleJsonString() {
            return "in";
        }
    }

    private static class SingleParseContext extends AbstractParseContext {
        private final String json;

        public SingleParseContext(JsonFactory factory, String json, String offsetInJsonPointer) {
            super(createParser(factory, json, Optional.ofNullable(offsetInJsonPointer)));
            this.json = json;
        }

        private static com.fasterxml.jackson.core.JsonParser createParser(
                JsonFactory factory, String json, Optional<String> offsetInJsonPointer) {
            try {
                final com.fasterxml.jackson.core.JsonParser baseParser = factory.createParser(json);
                return offsetInJsonPointer.map(p -> wrapWithPointerFilter(baseParser, p)).orElse(baseParser);
            } catch (Exception ex) {
                throw new JsonParseException("Failed to parse JSON: " + JsonParser.sampleJsonString(json), ex);
            }
        }

        public Value parse() {
            try {
                Value v = next();
                if (v == null) {
                    throw new JsonParseException("Unable to parse empty string");
                }
                return v;
            } catch (IOException ex) {
                throw new JsonParseException("Failed to parse JSON: " + sampleJsonString(), ex);
            }
        }

        @Override
        protected String sampleJsonString() {
            return JsonParser.sampleJsonString(json);
        }
    }

    private abstract static class AbstractParseContext {
        protected final com.fasterxml.jackson.core.JsonParser parser;

        public AbstractParseContext(com.fasterxml.jackson.core.JsonParser parser) {
            this.parser = parser;
        }

        protected abstract String sampleJsonString();

        public Value next() throws IOException {
            try {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    return null;
                }
                return jsonTokenToValue(token);
            } catch (com.fasterxml.jackson.core.JsonParseException ex) {
                throw new JsonParseException("Failed to parse JSON: " + sampleJsonString(), ex);
            } catch (IOException ex) {
                throw ex;
            } catch (JsonParseException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new JsonParseException("Failed to parse JSON: " + sampleJsonString(), ex);
            }
        }

        @SuppressWarnings("checkstyle:FallThrough")
        private Value jsonTokenToValue(JsonToken token) throws IOException {
            switch (token) {
                case VALUE_NULL:
                    return ValueFactory.newNil();
                case VALUE_TRUE:
                    return ValueFactory.newBoolean(true);
                case VALUE_FALSE:
                    return ValueFactory.newBoolean(false);
                case VALUE_NUMBER_FLOAT:
                    return ValueFactory.newFloat(parser.getDoubleValue());
                case VALUE_NUMBER_INT:
                    try {
                        return ValueFactory.newInteger(parser.getLongValue());
                    } catch (com.fasterxml.jackson.core.JsonParseException ex) {
                        return ValueFactory.newInteger(parser.getBigIntegerValue());
                    }
                case VALUE_STRING:
                    return ValueFactory.newString(parser.getText());
                case START_ARRAY: {
                    List<Value> list = new ArrayList<>();
                    while (true) {
                        token = parser.nextToken();
                        if (token == JsonToken.END_ARRAY) {
                            return ValueFactory.newArray(list);
                        } else if (token == null) {
                            throw new JsonParseException(
                                    "Unexpected end of JSON at "
                                            + parser.getTokenLocation()
                                            + " while expecting an element of an array: "
                                            + sampleJsonString());
                        }
                        list.add(jsonTokenToValue(token));
                    }
                }
                // Never fall through from the previous branch of START_ARRAY.
                case START_OBJECT:
                    Map<Value, Value> map = new HashMap<>();
                    while (true) {
                        token = parser.nextToken();
                        if (token == JsonToken.END_OBJECT) {
                            return ValueFactory.newMap(map);
                        } else if (token == null) {
                            throw new JsonParseException(
                                    "Unexpected end of JSON at "
                                            + parser.getTokenLocation()
                                            + " while expecting a key of object: "
                                            + sampleJsonString());
                        }
                        String key = parser.getCurrentName();
                        if (key == null) {
                            throw new JsonParseException(
                                    "Unexpected token "
                                            + token
                                            + " at "
                                            + parser.getTokenLocation()
                                            + ": "
                                            + sampleJsonString());
                        }
                        token = parser.nextToken();
                        if (token == null) {
                            throw new JsonParseException(
                                    "Unexpected end of JSON at "
                                            + parser.getTokenLocation()
                                            + " while expecting a value of object: "
                                            + sampleJsonString());
                        }
                        Value value = jsonTokenToValue(token);
                        map.put(ValueFactory.newString(key), value);
                    }
                // Never fall through from the previous branch of START_OBJECT.
                case VALUE_EMBEDDED_OBJECT:
                case FIELD_NAME:
                case END_ARRAY:
                case END_OBJECT:
                case NOT_AVAILABLE:
                default:
                    throw new JsonParseException("Unexpected token " + token + " at " + parser.getTokenLocation() + ": " + sampleJsonString());
            }
        }
    }
}
