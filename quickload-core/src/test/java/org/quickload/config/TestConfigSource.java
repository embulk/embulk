package org.quickload.config;

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
import org.quickload.exec.ExecModule;
import org.quickload.GuiceJUnitRunner;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ ExecModule.class })
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
        @Config("in:boolean")
        public boolean getBoolean();

        @Config("in:byte_array")
        public byte[] getByteArray();

        @Config("in:double")
        public double getDouble();

        @Config("in:int")
        public int getInt();

        @Config("in:long")
        public long getLong();

        @Config("in:string")
        public String getString();
    }

    @Test
    public void testLoadConfig()
    {
        config.putBoolean("in:boolean", true);
        config.putByteArray("in:byte_array", new byte[] { (byte) 0xff, (byte) 0xfe } );
        config.putInt("in:int", 3);
        config.putDouble("in:double", 0.2);
        config.putLong("in:long", Long.MAX_VALUE);
        config.putString("in:string", "sf");

        TypeFields task = config.loadModel(modelManager, TypeFields.class);
        assertEquals(true, task.getBoolean());
        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xfe }, task.getByteArray());
        assertEquals(3, task.getInt());
        assertEquals(0.2, task.getDouble(), 0.001);
        assertEquals(Long.MAX_VALUE, task.getLong());
        assertEquals("sf", task.getString());
    }

    @Test
    public void testValidate()
    {
        // TODO
    }

    @Test
    public void testDefaultValue()
    {
        // TODO
    }

    @Test
    public void testFromJson()
    {
        // TODO
    }
}
