package org.embulk.deps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDependencyCategory {
    /**
     * Tests the constants as a design contract.
     *
     * The manifest attribute name ({@link DependencyCategory#getManifestAttributeName}) is used to
     * generate an executable JAR file. When changing / adding constant(s), remember to take care of
     * the attribute names written in {@code build.gradle} as well.
     */
    @Test
    public void testConstants() {
        assertEquals(4, DependencyCategory.values().length);
        assertEquals("CLI", DependencyCategory.CLI.getName());
        assertEquals("Embulk-Resource-Class-Path-Cli", DependencyCategory.CLI.getManifestAttributeName());
        assertEquals("Maven", DependencyCategory.MAVEN.getName());
        assertEquals("Embulk-Resource-Class-Path-Maven", DependencyCategory.MAVEN.getManifestAttributeName());
        assertEquals("Buffer", DependencyCategory.BUFFER.getName());
        assertEquals("Embulk-Resource-Class-Path-Buffer", DependencyCategory.BUFFER.getManifestAttributeName());
        assertEquals("Config", DependencyCategory.CONFIG.getName());
        assertEquals("Embulk-Resource-Class-Path-Config", DependencyCategory.CONFIG.getManifestAttributeName());
    }
}
