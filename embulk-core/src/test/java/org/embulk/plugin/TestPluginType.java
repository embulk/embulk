package org.embulk.plugin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestPluginType
{
    @Test
    public void testEquals()
    {
        PluginType type = new PluginType("a");
        assertEquals(true, (type.equals(type)));

        assertEquals(true, (type.equals(new PluginType("a"))));
        assertEquals(false, (type.equals(new PluginType("b"))));
    }
}
