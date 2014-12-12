package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.embulk.record.PageTestUtils.newColumn;
import static org.embulk.record.PageTestUtils.newSchema;
import static org.embulk.record.Types.BOOLEAN;
import static org.embulk.record.Types.DOUBLE;
import static org.embulk.record.Types.LONG;
import static org.embulk.record.Types.STRING;
import static org.embulk.record.Types.TIMESTAMP;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.embulk.GuiceJUnitRunner;
import org.embulk.TestRuntimeBinder;
import org.embulk.TestUtilityModule;
import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageChannel;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.exec.BufferManager;
import org.embulk.plugin.MockPluginSource;
import org.embulk.record.PageBuilder;
import org.embulk.record.RecordWriter;
import org.embulk.record.Schema;
import org.embulk.spi.FileOutputPlugin.OutputTask;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class TestFileOutputPlugin
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private static interface TestFormatterTask extends Task
    {
        @Config("field")
        public String getField();
    }

    private static class TestTargetFileOutputPlugin extends FileOutputPlugin
    {
        boolean emulateException = false;

        @Override
        public NextConfig runFileOutputTransaction(ExecTask exec,
                ConfigSource config, ExecControl control)
        {
            control.run(new TaskSource());
            return new NextConfig();
        }

        @Override
        public Report runFileOutput(ExecTask exec, TaskSource taskSource,
                int processorIndex, FileBufferInput fileBufferInput)
        {
            if (emulateException) {
                throw new RuntimeException("emulated exception");
            }
            return new Report();
        }
    }

    @Test
    public void testTransactionSetsFormatterTask()
    {
        // prepare
        MockFormatterPlugin formatter = new MockFormatterPlugin(
                new ArrayList<List<Buffer>>(), TestFormatterTask.class);
        // always use MockFormatterPlugin as "formatter" plugin
        binder.addModule(MockPluginSource.newInjectModule(
                FormatterPlugin.class, formatter));

        // run
        ExecTask exec = binder.newExecTask();
        ConfigSource config = new ConfigSource().set(
                "formatter",
                new ConfigSource().setString("field", "frsyuki").setString(
                        "type", "dummy"));
        TestTargetFileOutputPlugin plugin = binder
                .getInstance(TestTargetFileOutputPlugin.class);
        MockExecControl control = new MockExecControl();
        plugin.runOutputTransaction(exec, config, control);

        // test
        FileOutputPlugin.OutputTask outputTask = exec.loadTask(
                control.getTaskSource(), FileOutputPlugin.OutputTask.class);
        TestFormatterTask formatterTask = exec.loadTask(
                outputTask.getFormatterTask(), TestFormatterTask.class);
        assertEquals("frsyuki", formatterTask.getField());
    }

    @Test
    public void testRunOutput()
    {
        // it does nothing but captures given records
        MockFormatterPlugin formatter = new MockFormatterPlugin(
                new ArrayList<List<Buffer>>(), TestFormatterTask.class);
        // always use MockFormatterPlugin as "formatter" plugin
        binder.addModule(MockPluginSource.newInjectModule(
                FormatterPlugin.class, formatter));

        Schema schema = newSchema(newColumn("c1", BOOLEAN),
                newColumn("c2", LONG), newColumn("c3", DOUBLE),
                newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
        ExecTask exec = binder.newExecTask();
        exec.setSchema(schema);
        OutputTask task = prepareOutputTask(exec);

        TestTargetFileOutputPlugin plugin = binder
                .getInstance(TestTargetFileOutputPlugin.class);

        PageChannel channel = new PageChannel(Integer.MAX_VALUE);
        try {
            // prepare pages
            MockPageInput pageInput = preparePageInput(schema, channel);
            // run plugin
            plugin.runOutput(exec, exec.dumpTask(task), 0, pageInput);
            // test
            assertEquals(3, formatter.getRecords().size());
        } finally {
            channel.close();
        }
    }

    @Test(expected = RuntimeException.class)
    public void testRunOutputPropagatesException()
    {
        // it does nothing but captures given records
        MockFormatterPlugin formatter = new MockFormatterPlugin(
                new ArrayList<List<Buffer>>(), TestFormatterTask.class);
        // always use MockFormatterPlugin as "formatter" plugin
        binder.addModule(MockPluginSource.newInjectModule(
                FormatterPlugin.class, formatter));

        Schema schema = newSchema(newColumn("c1", BOOLEAN),
                newColumn("c2", LONG), newColumn("c3", DOUBLE),
                newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
        ExecTask exec = binder.newExecTask();
        exec.setSchema(schema);
        OutputTask task = prepareOutputTask(exec);

        TestTargetFileOutputPlugin plugin = binder
                .getInstance(TestTargetFileOutputPlugin.class);

        PageChannel channel = new PageChannel(Integer.MAX_VALUE);
        try {
            // prepare pages
            MockPageInput pageInput = preparePageInput(schema, channel);
            // run plugin
            plugin.emulateException = true;
            plugin.runOutput(exec, exec.dumpTask(task), 0, pageInput);
        } finally {
            channel.close();
        }
    }

    private MockPageInput preparePageInput(Schema schema, PageChannel channel)
    {
        PageBuilder builder = new PageBuilder(
                binder.getInstance(BufferManager.class), schema,
                channel.getOutput());
        RecordWriter dummyRecordWriter = new MockRecordWriter();
        builder.addRecord(dummyRecordWriter);
        builder.addRecord(dummyRecordWriter);
        builder.addRecord(dummyRecordWriter);
        builder.close();
        channel.completeProducer();
        MockPageInput pageInput = new MockPageInput(channel.getInput()
                .iterator());
        return pageInput;
    }

    private OutputTask prepareOutputTask(ExecTask exec)
    {
        ConfigSource config = new ConfigSource().set("formatter",
                new ConfigSource().setString("type", "dummy"));
        OutputTask task = exec.loadConfig(config, OutputTask.class);
        task.setFormatterTask(new TaskSource());
        task.setFileOutputTask(new TaskSource());
        return task;
    }
}
