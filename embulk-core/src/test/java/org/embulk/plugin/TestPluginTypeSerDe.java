package org.embulk.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.embulk.EmbulkTestRuntime;
import org.junit.Rule;
import org.junit.Test;

public class TestPluginTypeSerDe {
    @Rule
    public EmbulkTestRuntime testRuntime = new EmbulkTestRuntime();

    @Test
    public void testParseTypeString() {
        PluginType pluginType = testRuntime.getModelManager().readObjectWithConfigSerDe(
                PluginType.class,
                "\"file\"");
        assertTrue(pluginType instanceof DefaultPluginType);
        assertEquals(PluginSource.Type.DEFAULT, pluginType.getSourceType());
        assertEquals("file", pluginType.getName());

        assertEquals("\"file\"", testRuntime.getModelManager().writeObject(pluginType));
    }

    @Test
    public void testParseTypeMapping() {
        PluginType pluginType = testRuntime.getModelManager().readObjectWithConfigSerDe(
                PluginType.class,
                "{ \"name\": \"dummy\" }");
        assertTrue(pluginType instanceof DefaultPluginType);
        assertEquals(PluginSource.Type.DEFAULT, pluginType.getSourceType());
        assertEquals("dummy", pluginType.getName());

        assertEquals("\"dummy\"", testRuntime.getModelManager().writeObject(pluginType));
    }

    @Test
    public void testParseTypeMaven() {
        PluginType pluginType = testRuntime.getModelManager().readObjectWithConfigSerDe(
                PluginType.class,
                "{ \"name\": \"foo\", \"source\": \"maven\", \"group\": \"org.embulk.bar\", \"version\": \"0.1.2\" }");
        assertTrue(pluginType instanceof MavenPluginType);
        assertEquals(PluginSource.Type.MAVEN, pluginType.getSourceType());
        MavenPluginType mavenPluginType = (MavenPluginType) pluginType;
        assertEquals(mavenPluginType.getName(), "foo");
        assertEquals(mavenPluginType.getGroup(), "org.embulk.bar");
        assertEquals(mavenPluginType.getVersion(), "0.1.2");
        assertNull(mavenPluginType.getClassifier());

        // Serializing MavenPluginType has been failing unintentionally.
        try {
            testRuntime.getModelManager().writeObject(pluginType);
        } catch (final RuntimeException ex) {
            assertTrue(ex.getCause() instanceof JsonMappingException);
            return;
        }
        fail();
    }

    @Test
    public void testParseTypeMavenWithClassifier() {
        PluginType pluginType = testRuntime.getModelManager().readObjectWithConfigSerDe(
                PluginType.class,
                "{ \"name\": \"foo\", \"source\": \"maven\", \"group\": \"org.embulk.bar\", \"version\": \"0.1.2\", \"classifier\": \"foo\" }");
        assertTrue(pluginType instanceof MavenPluginType);
        assertEquals(PluginSource.Type.MAVEN, pluginType.getSourceType());
        MavenPluginType mavenPluginType = (MavenPluginType) pluginType;
        assertEquals(mavenPluginType.getName(), "foo");
        assertEquals(mavenPluginType.getGroup(), "org.embulk.bar");
        assertEquals(mavenPluginType.getVersion(), "0.1.2");
        assertEquals(mavenPluginType.getClassifier(), "foo");

        // Serializing MavenPluginType has been failing unintentionally.
        try {
            testRuntime.getModelManager().writeObject(pluginType);
        } catch (final RuntimeException ex) {
            assertTrue(ex.getCause() instanceof JsonMappingException);
            return;
        }
        fail();
    }
}
