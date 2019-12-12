package org.embulk.deps.yaml;

import org.junit.Test;

import static org.junit.Assert.*;

public class YamlProcessorTest
{
    @Test
    public void create()
    {
        assertTrue(YamlProcessor.create(false) instanceof YamlProcessorImpl);
        assertTrue(YamlProcessor.create(true) instanceof YamlProcessorImpl);
    }
}