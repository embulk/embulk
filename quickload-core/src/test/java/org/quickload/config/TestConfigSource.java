package org.quickload.config;

import javax.validation.constraints.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.runner.RunWith;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.GuiceJUnitRunner;
import org.quickload.TestRuntimeModule;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestRuntimeModule.class })
public class TestConfigSource
{
    @Inject
    protected ModelManager modelManager;

    private ConfigSource config;

    @Before
    public void setup() throws Exception
    {
        config = new ConfigSource();
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
        config.setBoolean("boolean", true);
        config.setInt("int", 3);
        config.setDouble("double", 0.2);
        config.setLong("long", Long.MAX_VALUE);
        config.setString("string", "sf");

        assertEquals(true, config.getBoolean("boolean"));
        assertEquals(3, config.getInt("int"));
        assertEquals(0.2, config.getDouble("double"), 0.001);
        assertEquals(Long.MAX_VALUE, config.getLong("long"));
        assertEquals("sf", config.getString("string"));
    }

    @Test
    public void testLoadConfig()
    {
        config.setBoolean("boolean", true);
        config.setInt("int", 3);
        config.setDouble("double", 0.2);
        config.setLong("long", Long.MAX_VALUE);
        config.setString("string", "sf");

        TypeFields task = config.loadModel(modelManager, TypeFields.class);
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
        @NotNull
        public String getValid();
    }

    @Test
    public void testValidatePasses()
    {
        config.setString("valid", "data");
        ValidateFields task = config.loadModel(modelManager, ValidateFields.class);
        task.validate();
        assertEquals("data", task.getValid());
    }

    @Test(expected = ConfigException.class)
    public void testDefaultValueValidateFails()
    {
        ValidateFields task = config.loadModel(modelManager, ValidateFields.class);
        task.validate();
    }

    // TODO test Min, Max, and other validations

    private static interface SimpleFields
            extends Task
    {
        @Config("type")
        @NotNull
        public String getType();
    }

    @Test
    public void testFromJson()
    {
        String json = "{\"type\":\"test\"}";
        // TODO
    }
}
