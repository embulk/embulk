package org.embulk.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import org.junit.Test;

public class TestPluginType
{
    @Test
    public void testEquals()
    {
        PluginType type = PluginType.createFromStringForTesting("a");
        assertTrue(type instanceof DefaultPluginType);
        assertTrue(type.equals(type));

        assertTrue(type.equals(PluginType.createFromStringForTesting("a")));
        assertFalse(type.equals(PluginType.createFromStringForTesting("b")));
    }

    @Test
    public void testMapping1()
    {
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("source", "default");
        mapping.put("name", "c");

        PluginType type = PluginType.createFromStringMapForTesting(mapping);
        assertTrue(type instanceof DefaultPluginType);
        assertTrue(type.equals(type));

        assertTrue(type.equals(PluginType.createFromStringForTesting("c")));
        assertFalse(type.equals(PluginType.createFromStringForTesting("d")));
    }
}
