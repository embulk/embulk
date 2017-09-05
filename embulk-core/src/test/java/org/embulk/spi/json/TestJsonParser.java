package org.embulk.spi.json;

import com.google.common.collect.Lists;
import org.embulk.EmbulkTestRuntime;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.value.FloatValue;
import org.msgpack.value.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.msgpack.value.ValueFactory.newArray;
import static org.msgpack.value.ValueFactory.newBoolean;
import static org.msgpack.value.ValueFactory.newFloat;
import static org.msgpack.value.ValueFactory.newInteger;
import static org.msgpack.value.ValueFactory.newMap;
import static org.msgpack.value.ValueFactory.newNil;
import static org.msgpack.value.ValueFactory.newString;

public class TestJsonParser
{
    private Random random;
    private JsonParser parser;

    @Before
    public void createJsonParser()
    {
        random = new EmbulkTestRuntime().getRandom();
        parser = new JsonParser();
    }

    @Test
    public void testNil()
            throws Exception
    {
        testWithParseContexts(newNil(), "null");
    }

    @Test
    public void testBoolean()
            throws Exception
    {
        testWithParseContexts(newBoolean(true), "true");
        testWithParseContexts(newBoolean(false), "false");
    }

    @Test
    public void testFloat()
            throws Exception
    {
        Assertion assertion = new Assertion() {
            @Override
            public void equal(Value expected, Value actual)
            {
                assertEquals(((FloatValue)expected).toFloat(), ((FloatValue)actual).toFloat(), 0.0001);
            }
        };

        testWithParseContexts(newFloat(0.0), "0.0", assertion);
        for (int i = 0; i < 10; i++) {
            float f = random.nextFloat();
            testWithParseContexts(newFloat(f), Float.toString(f), assertion);
        }
    }

    @Test
    public void testInt()
            throws Exception
    {
        testWithParseContexts(newInteger(0), "0");
        testWithParseContexts(newInteger(1), "1");
        testWithParseContexts(newInteger(-1), "-1");
        testWithParseContexts(newInteger(Integer.MAX_VALUE), Integer.toString(Integer.MAX_VALUE));
        testWithParseContexts(newInteger(Integer.MIN_VALUE), Integer.toString(Integer.MIN_VALUE));
        for (int i = 0; i < 10; i++) {
            int integer = random.nextInt();
            testWithParseContexts(newInteger(integer), Integer.toString(integer));
        }
    }

    @Test
    public void testArray()
            throws Exception
    {
        testWithParseContexts(newArray(newInteger(0), newString("embulk")), "[0, \"embulk\"]");
    }

    @Test
    public void testObject()
            throws Exception
    {
        testWithParseContexts(newMap(newString("c0"), newString("embulk"), newString("c1"), newInteger(10)), "{\"c0\":\"embulk\",\"c1\":10}");
        testWithParseContexts(newMap(newString("c0"), newArray(newInteger(0), newString("embulk"))), "{\"c0\":[0,\"embulk\"]}");
    }

    @Test
    public void testInvalidJson()
    {
        List<String> invalidJsons = Lists.newArrayList("[\"invalid\"", "]", "{\"invalid\"", "}");
        for (String invalidJson : invalidJsons) {
            try {
                testWithSingleParseContext(null, invalidJson);
                fail();
            }
            catch (Throwable t) {
                assertTrue(t instanceof JsonParseException);
            }
            try {
                testWithStreamParseContext(null, invalidJson);
                fail();
            }
            catch (Throwable t) {
                assertTrue(t instanceof JsonParseException);
            }
        }
    }

    private void testWithParseContexts(Value expected, String json)
            throws Exception
    {
        testWithSingleParseContext(expected, json);
        testWithStreamParseContext(expected, json);
    }

    private void testWithParseContexts(Value expected, String json, Assertion assertion)
            throws Exception
    {
        testWithSingleParseContext(expected, json, assertion);
        testWithStreamParseContext(expected, json, assertion);
    }

    private void testWithSingleParseContext(Value expected, String json)
    {
        testWithSingleParseContext(expected, json, new Assertion() {
            @Override
            public void equal(Value expected, Value actual)
            {
                assertEquals(expected, actual);
            }
        });
    }

    private void testWithSingleParseContext(Value expected, String json, Assertion asssertion)
    {
        asssertion.equal(expected, new JsonParser().parse(json));
    }

    private void testWithStreamParseContext(Value expected, String json)
            throws IOException
    {
        testWithStreamParseContext(expected, json, new Assertion() {
            @Override
            public void equal(Value expected, Value actual)
            {
                assertEquals(expected, actual);
            }
        });
    }

    private void testWithStreamParseContext(Value expected, String json, Assertion assertion)
            throws IOException
    {
        try (InputStream in = new ByteArrayInputStream(json.getBytes());
                JsonParser.Stream stream = parser.open(in)) {
            assertion.equal(expected, stream.next());
        }
    }

    interface Assertion
    {
        void equal(Value expected, Value actual);
    }
}
