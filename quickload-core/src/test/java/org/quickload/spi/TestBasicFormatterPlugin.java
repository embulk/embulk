package org.quickload.spi;

import static org.junit.Assert.assertEquals;
import static org.quickload.record.PageTestUtils.newColumn;
import static org.quickload.record.PageTestUtils.newSchema;
import static org.quickload.record.Types.BOOLEAN;
import static org.quickload.record.Types.DOUBLE;
import static org.quickload.record.Types.LONG;
import static org.quickload.record.Types.STRING;
import static org.quickload.record.Types.TIMESTAMP;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quickload.GuiceJUnitRunner;
import org.quickload.TestRuntimeBinder;
import org.quickload.TestUtilityModule;
import org.quickload.channel.FileBufferChannel;
import org.quickload.channel.FileBufferOutput;
import org.quickload.channel.PageChannel;
import org.quickload.channel.PageInput;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.exec.BufferManager;
import org.quickload.plugin.MockPluginSource;
import org.quickload.record.PageBuilder;
import org.quickload.record.PageReader;
import org.quickload.record.RecordWriter;
import org.quickload.record.Schema;
import org.quickload.spi.FileOutputPlugin.OutputTask;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class TestBasicFormatterPlugin
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private static class TestTargetBasicFormatterPlugin extends
            BasicFormatterPlugin
    {
        boolean emulateException = false;
        List<List<Object>> records;

        @Override
        public TaskSource getBasicFormatterTask(ExecTask exec,
                ConfigSource config)
        {
            // non configurable
            return new TaskSource();
        }

        @Override
        public void runBasicFormatter(ExecTask exec, TaskSource taskSource,
                int processorIndex, PageInput pageInput,
                FileBufferOutput fileBufferOutput)
        {
            final MockRecordReader recordReader = new MockRecordReader(
                    fileBufferOutput);
            try (PageReader reader = new PageReader(exec.getSchema(), pageInput)) {
                while (reader.nextRecord()) {
                    reader.visitColumns(recordReader);
                    recordReader.addRecord();
                }
            }
            records = recordReader.getRecords();
            fileBufferOutput.addFile();
            if (emulateException) {
                throw new RuntimeException("emulated exception");
            }
        }
    }

    @Test
    public void testRunFormatter()
    {
        // force using TestTargetFileEncoderPlugin
        MockToStringEncoderPlugin encoderPlugin = new MockToStringEncoderPlugin();
        binder.addModule(MockPluginSource.newInjectModule(
                FileEncoderPlugin.class, encoderPlugin));

        TestTargetBasicFormatterPlugin plugin = new TestTargetBasicFormatterPlugin();

        Schema schema = newSchema(newColumn("c1", BOOLEAN),
                newColumn("c2", LONG), newColumn("c3", DOUBLE),
                newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
        ExecTask exec = binder.newExecTask();
        exec.setSchema(schema);

        PageChannel inputChannel = new PageChannel(Integer.MAX_VALUE);
        FileBufferChannel outputChannel = exec.newFileBufferChannel();
        try {
            // prepare pages
            MockPageInput pageInput = preparePageInput(schema, inputChannel);
            TaskSource task = prepareFormatterTask(exec, plugin);

            // run plugin
            plugin.runFormatter(exec, task, 0, pageInput,
                    outputChannel.getOutput());

            // test
            assertEquals(3, plugin.records.size());
            assertEquals(5, plugin.records.get(0).size());
            // encoded to 3 buffers in 1 file.
            assertEquals(1, encoderPlugin.getFiles().size());
            assertEquals(3, encoderPlugin.getFiles().get(0).size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    @Test(expected = RuntimeException.class)
    public void testRunFormatterPropagatesException()
    {
        // force using TestTargetFileEncoderPlugin
        MockToStringEncoderPlugin encoderPlugin = new MockToStringEncoderPlugin();
        binder.addModule(MockPluginSource.newInjectModule(
                FileEncoderPlugin.class, encoderPlugin));

        TestTargetBasicFormatterPlugin plugin = new TestTargetBasicFormatterPlugin();
        plugin.emulateException = true;

        Schema schema = newSchema(newColumn("c1", BOOLEAN),
                newColumn("c2", LONG), newColumn("c3", DOUBLE),
                newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
        ExecTask exec = binder.newExecTask();
        exec.setSchema(schema);

        PageChannel inputChannel = new PageChannel(Integer.MAX_VALUE);
        FileBufferChannel outputChannel = exec.newFileBufferChannel();
        try {
            // prepare pages
            MockPageInput pageInput = preparePageInput(schema, inputChannel);
            TaskSource task = prepareFormatterTask(exec, plugin);

            // run plugin
            plugin.runFormatter(exec, task, 0, pageInput,
                    outputChannel.getOutput());
        } finally {
            inputChannel.close();
            outputChannel.close();
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

    private TaskSource prepareFormatterTask(ExecTask exec,
            TestTargetBasicFormatterPlugin plugin)
    {
        // Apply same encoder twice for testing.
        ConfigSource config = new ConfigSource().set(
                "formatter",
                new ConfigSource().setString("type", "dummy").set(
                        "file_encoders",
                        ConfigSource
                                .arrayNode()
                                .add(ConfigSource.objectNode()
                                        .put("type", "dummy2")
                                        .put("prefix", "nahi"))
                                .add(ConfigSource.objectNode()
                                        .put("type", "dummy3")
                                        .put("prefix", "71"))));
        OutputTask outputTask = exec.loadConfig(config, OutputTask.class);
        outputTask.setFormatterTask(plugin.getFormatterTask(exec,
                outputTask.getFormatterConfig()));
        return outputTask.getFormatterTask();
    }
}
