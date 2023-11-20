/*
 * Copyright 2015 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.deps.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

/**
 * Parses a stringified JSON to MessagePack {@link org.msgpack.value.Value}.
 */
public class JsonParserDelegateImpl extends org.embulk.spi.json.JsonParserDelegate {
    /**
     * Creates a {@link JsonParserDelegateImpl} instance.
     */
    public JsonParserDelegateImpl() {
        this.factory = new JsonFactory();
        factory.enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        factory.enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
    }

    /**
     * A parsed stream of MessagePack {@link org.msgpack.value.Value}s.
     */
    public interface Stream extends org.embulk.spi.json.JsonParser.Stream {
        /**
         * Gets the next MessagePack {@link org.msgpack.value.Value}.
         *
         * @return parsed {@link org.msgpack.value.Value}
         */
        @Override
        Value next() throws IOException;

        /**
         * Closes the stream.
         */
        @Override
        void close() throws IOException;
    }

    /**
     * Parses the stringified JSON {@link java.io.InputStream} to {@link Stream}.
     *
     * @param in  stringified JSON {@link java.io.InputStream} to parse
     * @return a stream of parsed {@link org.msgpack.value.Value}
     */
    @Override
    public Stream open(final InputStream in) throws IOException {
        return openWithOffsetInJsonPointer(in, null);
    }

    /**
     * Parses the stringified JSON {@link java.io.InputStream} with the specified offset to {@link Stream}.
     *
     * @param in  stringified JSON {@link java.io.InputStream} to parse
     * @param offsetInJsonPointer  offset in JSON Pointer to parse
     * @return a stream of parsed {@link org.msgpack.value.Value}
     */
    @Override
    public Stream openWithOffsetInJsonPointer(final InputStream in, final String offsetInJsonPointer) throws IOException {
        return new StreamParseContext(factory, in, offsetInJsonPointer);
    }

    /**
     * Parses the stringified JSON {@link java.lang.String} to {@link org.msgpack.value.Value}.
     *
     * @param json  stringified JSON to parse
     * @return parsed {@link org.msgpack.value.Value}
     */
    @Override
    public Value parse(final String json) {
        return parseWithOffsetInJsonPointer(json, null);
    }

    /**
     * Parses the stringified JSON {@link java.lang.String} with the specified offset to {@link org.msgpack.value.Value}.
     *
     * @param json  stringified JSON to parse
     * @param offsetInJsonPointer  offset in JSON Pointer to parse
     * @return parsed {@link org.msgpack.value.Value}
     */
    @Override
    public Value parseWithOffsetInJsonPointer(final String json, final String offsetInJsonPointer) {
        return new SingleParseContext(factory, json, offsetInJsonPointer).parse();
    }

    private static String sampleJsonString(final String json) {
        if (json.length() < 100) {
            return json;
        } else {
            return json.substring(0, 97) + "...";
        }
    }

    private static com.fasterxml.jackson.core.JsonParser wrapWithPointerFilter(
            final com.fasterxml.jackson.core.JsonParser baseParser, final String offsetInJsonPointer) {
        return new FilteringParserDelegate(
                baseParser,
                new JsonPointerBasedFilter(offsetInJsonPointer),
                false,
                true  // Allow multiple matches
                );
    }

    private static class StreamParseContext extends AbstractParseContext implements Stream {
        public StreamParseContext(
                final JsonFactory factory, final InputStream in, final String offsetInJsonPointer) throws IOException {
            super(createParser(factory, in, Optional.ofNullable(offsetInJsonPointer)));
        }

        private static com.fasterxml.jackson.core.JsonParser createParser(
                final JsonFactory factory, final InputStream in, final Optional<String> offsetInJsonPointer) throws IOException {
            try {
                final com.fasterxml.jackson.core.JsonParser baseParser = factory.createParser(in);
                return offsetInJsonPointer.map(p -> wrapWithPointerFilter(baseParser, p)).orElse(baseParser);
            } catch (final IOException ex) {
                throw ex;
            } catch (final Exception ex) {
                throw new org.embulk.spi.json.JsonParseException("Failed to parse JSON", ex);
            }
        }

        @Override
        public void close() throws IOException {
            this.closeParser();
        }

        @Override
        protected String sampleJsonString() {
            return "in";
        }
    }

    private static class SingleParseContext extends AbstractParseContext {
        public SingleParseContext(final JsonFactory factory, final String json, final String offsetInJsonPointer) {
            super(createParser(factory, json, Optional.ofNullable(offsetInJsonPointer)));
            this.json = json;
        }

        private static com.fasterxml.jackson.core.JsonParser createParser(
                final JsonFactory factory, final String json, final Optional<String> offsetInJsonPointer) {
            try {
                final com.fasterxml.jackson.core.JsonParser baseParser = factory.createParser(json);
                return offsetInJsonPointer.map(p -> wrapWithPointerFilter(baseParser, p)).orElse(baseParser);
            } catch (final Exception ex) {
                throw new org.embulk.spi.json.JsonParseException("Failed to parse JSON: " + JsonParserDelegateImpl.sampleJsonString(json), ex);
            }
        }

        public Value parse() {
            try {
                final Value v = this.next();
                if (v == null) {
                    throw new org.embulk.spi.json.JsonParseException("Unable to parse empty string");
                }
                return v;
            } catch (final IOException ex) {
                throw new org.embulk.spi.json.JsonParseException("Failed to parse JSON: " + sampleJsonString(), ex);
            }
        }

        @Override
        protected String sampleJsonString() {
            return JsonParserDelegateImpl.sampleJsonString(this.json);
        }

        private final String json;
    }

    private abstract static class AbstractParseContext {
        public AbstractParseContext(final com.fasterxml.jackson.core.JsonParser parser) {
            this.parser = parser;
        }

        protected final void closeParser() throws IOException {
            this.parser.close();
        }

        protected abstract String sampleJsonString();

        public final Value next() throws IOException {
            try {
                final JsonToken token = this.parser.nextToken();
                if (token == null) {
                    return null;
                }
                return this.jsonTokenToValue(token);
            } catch (final com.fasterxml.jackson.core.JsonParseException ex) {
                throw new org.embulk.spi.json.JsonParseException("Failed to parse JSON: " + sampleJsonString(), ex);
            } catch (final IOException ex) {
                throw ex;
            } catch (final org.embulk.spi.json.JsonParseException ex) {
                throw ex;
            } catch (final RuntimeException ex) {
                throw new org.embulk.spi.json.JsonParseException("Failed to parse JSON: " + sampleJsonString(), ex);
            }
        }

        @SuppressWarnings("checkstyle:FallThrough")
        private final Value jsonTokenToValue(final JsonToken token) throws IOException {
            switch (token) {
                case VALUE_NULL:
                    return ValueFactory.newNil();
                case VALUE_TRUE:
                    return ValueFactory.newBoolean(true);
                case VALUE_FALSE:
                    return ValueFactory.newBoolean(false);
                case VALUE_NUMBER_FLOAT:
                    return ValueFactory.newFloat(this.parser.getDoubleValue());
                case VALUE_NUMBER_INT:
                    try {
                        return ValueFactory.newInteger(this.parser.getLongValue());
                    } catch (final com.fasterxml.jackson.core.JsonParseException ex) {
                        return ValueFactory.newInteger(this.parser.getBigIntegerValue());
                    }
                case VALUE_STRING:
                    return ValueFactory.newString(this.parser.getText());
                case START_ARRAY: {
                    final List<Value> list = new ArrayList<>();
                    while (true) {
                        final JsonToken nextToken = this.parser.nextToken();
                        if (nextToken == JsonToken.END_ARRAY) {
                            return ValueFactory.newArray(list);
                        } else if (nextToken == null) {
                            throw new org.embulk.spi.json.JsonParseException(
                                    "Unexpected end of JSON at "
                                            + this.parser.getTokenLocation()
                                            + " while expecting an element of an array: "
                                            + sampleJsonString());
                        }
                        list.add(this.jsonTokenToValue(nextToken));
                    }
                }
                // Never fall through from the previous branch of START_ARRAY.
                case START_OBJECT:
                    final Map<Value, Value> map = new HashMap<>();
                    while (true) {
                        final JsonToken nextToken = this.parser.nextToken();
                        if (nextToken == JsonToken.END_OBJECT) {
                            return ValueFactory.newMap(map);
                        } else if (nextToken == null) {
                            throw new org.embulk.spi.json.JsonParseException(
                                    "Unexpected end of JSON at "
                                            + this.parser.getTokenLocation()
                                            + " while expecting a key of object: "
                                            + sampleJsonString());
                        }
                        final String key = this.parser.getCurrentName();
                        if (key == null) {
                            throw new org.embulk.spi.json.JsonParseException(
                                    "Unexpected token "
                                            + nextToken
                                            + " at "
                                            + this.parser.getTokenLocation()
                                            + ": "
                                            + sampleJsonString());
                        }
                        final JsonToken nextNextToken = this.parser.nextToken();
                        if (nextNextToken == null) {
                            throw new org.embulk.spi.json.JsonParseException(
                                    "Unexpected end of JSON at "
                                            + this.parser.getTokenLocation()
                                            + " while expecting a value of object: "
                                            + sampleJsonString());
                        }
                        final Value value = this.jsonTokenToValue(nextNextToken);
                        map.put(ValueFactory.newString(key), value);
                    }
                // Never fall through from the previous branch of START_OBJECT.
                case VALUE_EMBEDDED_OBJECT:
                case FIELD_NAME:
                case END_ARRAY:
                case END_OBJECT:
                case NOT_AVAILABLE:
                default:
                    throw new org.embulk.spi.json.JsonParseException(
                            "Unexpected token " + token + " at " + this.parser.getTokenLocation() + ": " + sampleJsonString());
            }
        }

        private final com.fasterxml.jackson.core.JsonParser parser;
    }

    private final JsonFactory factory;
}
