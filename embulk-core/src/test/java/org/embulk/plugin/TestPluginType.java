package org.embulk.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
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
}
