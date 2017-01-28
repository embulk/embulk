package org.embulk.spi.json;

import com.fasterxml.jackson.core.JsonToken;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil
{
    // This class should be ported to msgpack-java in the future.

    public static Value readJson(com.fasterxml.jackson.core.JsonParser parser)
            throws IOException
    {
        try {
            JsonToken token = parser.nextToken();
            if (token == null) {
                return null;
            }
            return jsonTokenToValue(parser, token);
        }
        catch (com.fasterxml.jackson.core.JsonParseException e) {
            throw new JsonParseException("Failed to parse JSON", e);
        }
        catch (IOException e) {
            throw e;
        }
        catch (JsonParseException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new JsonParseException("Failed to parse JSON", e);
        }
    }

    public static Value jsonTokenToValue(com.fasterxml.jackson.core.JsonParser parser, JsonToken token)
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
        case START_ARRAY:
            List<Value> list = new ArrayList<>();
            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.END_ARRAY) {
                    return ValueFactory.newArray(list);
                }
                else if (token == null) {
                    throw new JsonParseException("Unexpected end of JSON at " + parser.getTokenLocation());
                }
                list.add(jsonTokenToValue(parser, token));
            }
        case START_OBJECT:
            Map<Value, Value> map = new HashMap<>();
            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.END_OBJECT) {
                    return ValueFactory.newMap(map);
                }
                else if (token == null) {
                    throw new JsonParseException("Unexpected end of JSON at " + parser.getTokenLocation());
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throw new JsonParseException("Unexpected token " + token + " at " + parser.getTokenLocation());
                }
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonParseException("Unexpected end of JSON at " + parser.getTokenLocation());
                }
                Value value = jsonTokenToValue(parser, token);
                map.put(ValueFactory.newString(key), value);
            }
        case VALUE_EMBEDDED_OBJECT:
        case FIELD_NAME:
        case END_ARRAY:
        case END_OBJECT:
        case NOT_AVAILABLE:
        default:
            throw new JsonParseException("Unexpected token " + token + " at " + parser.getTokenLocation());
        }
    }
}
