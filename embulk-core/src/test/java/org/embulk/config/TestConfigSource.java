package org.embulk.config;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import com.google.inject.Inject;
import org.embulk.spi.Exec;
import org.embulk.EmbulkTestRuntime;

public class TestConfigSource
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private ConfigSource config;

    @Before
    public void setup() throws Exception
    {
        config = Exec.newConfigSource();
    }

    private static interface TypeFields
            extends Task
    {
        @Config("boolean")
        public boolean getBoolean();

        @Config("double")
        public double getDouble();

        @Config("int")
        public int getInt();

        @Config("long")
        public long getLong();

        @Config("string")
        public String getString();
    }

    @Test
    public void testSetGet()
    {
        config.set("boolean", true);
        config.set("int", 3);
        config.set("double", 0.2);
        config.set("long", Long.MAX_VALUE);
        config.set("string", "sf");

        assertEquals(true, (boolean) config.get(boolean.class, "boolean"));
        assertEquals(3, (int) config.get(int.class, "int"));
        assertEquals(0.2, (double) config.get(double.class, "double"), 0.001);
        assertEquals(Long.MAX_VALUE, (long) config.get(long.class, "long"));
        assertEquals("sf", config.get(String.class, "string"));
    }

    @Test
    public void testLoadConfig()
    {
        config.set("boolean", true);
        config.set("int", 3);
        config.set("double", 0.2);
        config.set("long", Long.MAX_VALUE);
        config.set("string", "sf");

        TypeFields task = config.loadConfig(TypeFields.class);
        assertEquals(true, task.getBoolean());
        assertEquals(3, task.getInt());
        assertEquals(0.2, task.getDouble(), 0.001);
        assertEquals(Long.MAX_VALUE, task.getLong());
        assertEquals("sf", task.getString());
    }

    private static interface ValidateFields
            extends Task
    {
        @Config("valid")
        public String getValid();
    }

    @Test
    public void testValidatePasses()
    {
        config.set("valid", "data");
        ValidateFields task = config.loadConfig(ValidateFields.class);
        task.validate();
        assertEquals("data", task.getValid());
    }

    @Test(expected = ConfigException.class)
    public void testDefaultValueValidateFails()
    {
        ValidateFields task = config.loadConfig(ValidateFields.class);
        task.validate();
    }

    // TODO test Min, Max, and other validations

    private static interface SimpleFields
            extends Task
    {
        @Config("type")
        public String getType();
    }

    @Test
    public void testFromJson()
    {
        String json = "{\"type\":\"test\"}";
        // TODO
    }
}
