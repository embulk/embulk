package org.embulk.config;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class YamlProcessorTest {
    @Test
    public void create() {
        assertTrue(YamlProcessor.create(false) instanceof org.embulk.deps.config.YamlProcessorImpl);
        assertTrue(YamlProcessor.create(true) instanceof org.embulk.deps.config.YamlProcessorImpl);
    }
}
