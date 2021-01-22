package org.embulk.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Properties;
import org.embulk.EmbulkSystemProperties;
import org.junit.Test;

public class TestPluginType {
    @Test
    public void testEquals() {
        PluginType type = PluginType.createFromStringForTesting("a");
        assertTrue(type instanceof DefaultPluginType);
        assertEquals(PluginSource.Type.DEFAULT, type.getSourceType());
        assertTrue(type.equals(type));

        assertTrue(type.equals(PluginType.createFromStringForTesting("a")));
        assertFalse(type.equals(PluginType.createFromStringForTesting("b")));
    }

    @Test
    public void testMapping1() {
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("source", "default");
        mapping.put("name", "c");

        PluginType type = PluginType.createFromStringMapForTesting(mapping);
        assertTrue(type instanceof DefaultPluginType);
        assertEquals(PluginSource.Type.DEFAULT, type.getSourceType());
        assertTrue(type.equals(type));

        assertTrue(type.equals(PluginType.createFromStringForTesting("c")));
        assertFalse(type.equals(PluginType.createFromStringForTesting("d")));
    }

    @Test
    public void testMapping2() {
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("source", "maven");
        mapping.put("name", "e");
        mapping.put("group", "org.embulk.foobar");
        mapping.put("version", "0.1.2");

        PluginType type = PluginType.createFromStringMapForTesting(mapping);
        assertTrue(type instanceof MavenPluginType);
        assertEquals(PluginSource.Type.MAVEN, type.getSourceType());
        MavenPluginType mavenType = (MavenPluginType) type;
        assertTrue(mavenType.equals(mavenType));
        assertEquals("e", mavenType.getName());
        assertEquals("org.embulk.foobar", mavenType.getGroup());
        assertEquals("0.1.2", mavenType.getVersion());
        assertNull(mavenType.getClassifier());
        assertEquals("maven:org.embulk.foobar:e:0.1.2", mavenType.getFullName());
    }

    @Test
    public void testMappingMavenWithClassifier() {
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("source", "maven");
        mapping.put("name", "e");
        mapping.put("group", "org.embulk.foobar");
        mapping.put("version", "0.1.2");
        mapping.put("classifier", "bar");

        PluginType type = PluginType.createFromStringMapForTesting(mapping);
        assertTrue(type instanceof MavenPluginType);
        assertEquals(PluginSource.Type.MAVEN, type.getSourceType());
        MavenPluginType mavenType = (MavenPluginType) type;
        assertTrue(mavenType.equals(mavenType));
        assertEquals("e", mavenType.getName());
        assertEquals("org.embulk.foobar", mavenType.getGroup());
        assertEquals("0.1.2", mavenType.getVersion());
        assertEquals("bar", mavenType.getClassifier());
        assertEquals("maven:org.embulk.foobar:e:0.1.2:bar", mavenType.getFullName());
    }

    @Test
    public void testMavenFail1() {
        final DefaultPluginType type = (DefaultPluginType) PluginType.createFromStringForTesting("qux");

        try {
            MavenPluginType.createFromDefaultPluginType("input", type, EmbulkSystemProperties.of(new Properties()));
        } catch (final PluginSourceNotMatchException ex) {
            assertEquals("Embulk system property \"plugins.input.qux\" is not set.", ex.getMessage());
            return;
        }
        fail("PluginSourceNotMatchException is not thrown.");
    }

    @Test
    public void testMavenFail2() {
        final DefaultPluginType type = (DefaultPluginType) PluginType.createFromStringForTesting("foo");

        try {
            final Properties properties = new Properties();
            properties.setProperty("plugins.filter.foo", "maven:foo");
            MavenPluginType.createFromDefaultPluginType("filter", type, EmbulkSystemProperties.of(properties));
        } catch (final PluginSourceNotMatchException ex) {
            assertEquals("Embulk system property \"plugins.filter.foo\" is invalid: \"maven:foo\"",
                         ex.getMessage());
            return;
        }
        fail("PluginSourceNotMatchException is not thrown.");
    }

    @Test
    public void testMavenFail3() {
        final DefaultPluginType type = (DefaultPluginType) PluginType.createFromStringForTesting("test");

        try {
            final Properties properties = new Properties();
            properties.setProperty("plugins.output.test", "nonmaven:org.embulk.foo:test:0.2.4");
            MavenPluginType.createFromDefaultPluginType("output", type, EmbulkSystemProperties.of(properties));
        } catch (final PluginSourceNotMatchException ex) {
            assertEquals("Embulk system property \"plugins.output.test\" is invalid: \"nonmaven:org.embulk.foo:test:0.2.4\"",
                         ex.getMessage());
            return;
        }
        fail("PluginSourceNotMatchException is not thrown.");
    }

    @Test
    public void testMaven() throws PluginSourceNotMatchException {
        final DefaultPluginType type = (DefaultPluginType) PluginType.createFromStringForTesting("some");

        final Properties properties1 = new Properties();
        properties1.setProperty("plugins.input.some", "maven:org.embulk.baz:some:0.2.3");
        properties1.setProperty("plugins.filter.some", "maven:com.example:some:0.4.1:alpha");

        final MavenPluginType mavenType1 =
                MavenPluginType.createFromDefaultPluginType("input", type, EmbulkSystemProperties.of(properties1));
        assertEquals("some", mavenType1.getName());
        assertEquals("org.embulk.baz", mavenType1.getGroup());
        assertEquals("0.2.3", mavenType1.getVersion());
        assertEquals(null, mavenType1.getClassifier());
        assertEquals("maven:org.embulk.baz:some:0.2.3", mavenType1.getFullName());

        final MavenPluginType mavenType2 =
                MavenPluginType.createFromDefaultPluginType("filter", type, EmbulkSystemProperties.of(properties1));
        assertEquals("some", mavenType2.getName());
        assertEquals("com.example", mavenType2.getGroup());
        assertEquals("0.4.1", mavenType2.getVersion());
        assertEquals("alpha", mavenType2.getClassifier());
        assertEquals("maven:com.example:some:0.4.1:alpha", mavenType2.getFullName());
    }
}
