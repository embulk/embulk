package org.quickload.spi;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Rule;
import org.junit.runner.RunWith;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.config.TaskValidationException;
import org.quickload.record.Schema;
import org.quickload.channel.FileBufferOutput;
import org.quickload.GuiceJUnitRunner;
import org.quickload.GuiceBinder;
import org.quickload.TestRuntimeModule;
import org.quickload.TestUtilityModule;
import org.quickload.record.RandomSchemaGenerator;
import org.quickload.record.RandomRecordGenerator;

public class TestLineDecoderTask
{
    @Rule
    public GuiceBinder binder = new GuiceBinder(new TestRuntimeModule());

    @Test
    public void testDefaultValues()
    {
        ProcTask proc = new ProcTask(binder.getInjector());
        LineDecoderTask task = proc.loadConfig(new ConfigSource(), LineDecoderTask.class);
        assertEquals("utf-8", task.getEncoding());
        assertEquals("crlf", task.getNewline());
    }

    @Test
    public void testLoadConfig()
    {
        ProcTask proc = new ProcTask(binder.getInjector());
        ConfigSource config = new ConfigSource()
            .putString("in:encoding", "utf-16")
            .putString("in:newline", "lf");
        LineDecoderTask task = proc.loadConfig(config, LineDecoderTask.class);
        assertEquals("utf-16", task.getEncoding());
        assertEquals("lf", task.getNewline());
    }
}
