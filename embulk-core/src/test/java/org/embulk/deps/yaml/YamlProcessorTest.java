package org.embulk.deps.yaml;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class YamlProcessorTest {
    @Test
    public void create() {
        assertTrue(YamlProcessor.create(false) instanceof YamlProcessorImpl);
        assertTrue(YamlProcessor.create(true) instanceof YamlProcessorImpl);
    }
}