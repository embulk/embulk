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
import org.quickload.TestUtilityModule;
import org.quickload.TestRuntimeModule;
import org.quickload.record.RandomSchemaGenerator;
import org.quickload.record.RandomRecordGenerator;
import org.quickload.plugin.MockPluginSource;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class TestFileInputPlugin
{
    @Inject
    protected RandomSchemaGenerator schemaGen;
    @Inject
    protected RandomRecordGenerator recordGen;

    @Rule
    public GuiceBinder binder = new GuiceBinder(new TestRuntimeModule());

    private static interface TestParserTask
            extends Task
    {
        @Config("in:field")
        public String getField();
    }

    private static class ParserTestFileInputPlugin
                extends FileInputPlugin
    {
        public NextConfig runFileInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
        {
            control.run(new TaskSource());
            return new NextConfig();
        }

        public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
        {
            return new Report();
        }
    }

    @Test
    public void testTransactionSetsParserTask()
    {
        Schema schema = schemaGen.generate(60);
        MockParserPlugin parser = new MockParserPlugin(
                schema, ImmutableList.copyOf(recordGen.generate(schema, 100)),
                TestParserTask.class);
        binder.addModule(MockPluginSource.newInjectModule(ParserPlugin.class, parser));

        ProcTask proc = new ProcTask(binder.getInjector());

        ConfigSource config = new ConfigSource()
            .setString("in:parser_type", "dummy")
            .setString("in:field", "frsyuki");

        ParserTestFileInputPlugin plugin = binder.getInstance(ParserTestFileInputPlugin.class);

        MockProcControl control = new MockProcControl();
        plugin.runInputTransaction(proc, config, control);

        FileInputPlugin.InputTask inputTask =
            proc.loadTask(control.getTaskSource(), FileInputPlugin.InputTask.class);
        TestParserTask parserTask =
            proc.loadTask(inputTask.getParserTask(), TestParserTask.class);

        assertEquals("frsyuki", parserTask.getField());
    }

    // TODO testRunInput
}
