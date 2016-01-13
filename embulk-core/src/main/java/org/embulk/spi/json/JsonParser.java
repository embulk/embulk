package org.embulk.spi.json;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;

public class JsonParser
{
    private final JsonFactory factory;

    public JsonParser()
    {
        this.factory = new JsonFactory();
        factory.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
    }

    public Value parse(String json)
    {
        return new ParseContext(json).parse();
    }

    private class ParseContext
    {
        private final String json;
        private final com.fasterxml.jackson.core.JsonParser parser;

        public ParseContext(String json)
        {
            this.json = json;
            try {
                this.parser = factory.createParser(json);
            }
            catch (Exception ex) {
                throw new JsonParseException("Failed to parse a JSON string: "+sampleJsonString(json), ex);
            }
        }

        public Value parse()
        {
            try {
                JsonToken token = parser.nextToken();
                return jsonTokenToValue(token);
            }
            catch (JsonParseException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new JsonParseException("Failed to parse a JSON string: "+sampleJsonString(json), ex);
            }
        }

        private Value jsonTokenToValue(JsonToken token)
            throws IOException
        {
            switch(token) {
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
                }
                catch (JsonParseException ex) {
                    return ValueFactory.newInteger(parser.getBigIntegerValue());
                }
            case VALUE_STRING:
                return ValueFactory.newString(parser.getText());
            case START_ARRAY: {
                List<Value> list = new ArrayList<>();
                while (true) {
                    token = parser.nextToken();
                    if(token == JsonToken.END_ARRAY) {
                        return ValueFactory.newArray(list);
                    }
                    list.add(jsonTokenToValue(token));
                }
            }
            case START_OBJECT:
                Map<Value, Value> map = new HashMap<>();
                while (true) {
                    token = parser.nextToken();
                    if (token == JsonToken.END_OBJECT) {
                        return ValueFactory.newMap(map);
                    }
                    String key = parser.getCurrentName();
                    if (key == null) {
                        throw new JsonParseException("Unexpected token "+token+" at "+parser.getTokenLocation());
                    }
                    token = parser.nextToken();
                    Value value = jsonTokenToValue(token);
                    map.put(ValueFactory.newString(key), value);
                }
            case VALUE_EMBEDDED_OBJECT:
            case FIELD_NAME:
            case END_ARRAY:
            case END_OBJECT:
            case NOT_AVAILABLE:
            default:
                throw new JsonParseException("Unexpected token "+token+" at "+parser.getTokenLocation());
            }
        }
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
}
