package org.embulk.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.embulk.EmbulkTestRuntime;
import org.junit.Rule;
import org.junit.Test;

public class TestPluginTypeSerDe
{
    @Rule
    public EmbulkTestRuntime testRuntime = new EmbulkTestRuntime();

    @Test
    public void testParseTypeString()
    {
        PluginType pluginType = testRuntime.getModelManager().readObjectWithConfigSerDe(
            PluginType.class,
            "\"file\"");
        assertTrue(pluginType instanceof DefaultPluginType);
        assertEquals(pluginType.getName(), "file");
    }

    @Test
    public void testParseTypeMapping()
    {
        PluginType pluginType = testRuntime.getModelManager().readObjectWithConfigSerDe(
            PluginType.class,
            "{ \"name\": \"dummy\" }");
        assertTrue(pluginType instanceof DefaultPluginType);
        assertEquals(pluginType.getName(), "dummy");
    }
}
