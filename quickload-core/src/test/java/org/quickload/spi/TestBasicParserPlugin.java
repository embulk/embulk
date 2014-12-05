package org.quickload.spi;

import static org.quickload.record.PageTestUtils.newColumn;
import static org.quickload.record.PageTestUtils.newSchema;
import static org.quickload.record.Types.BOOLEAN;
import static org.quickload.record.Types.DOUBLE;
import static org.quickload.record.Types.LONG;
import static org.quickload.record.Types.STRING;
import static org.quickload.record.Types.TIMESTAMP;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quickload.GuiceJUnitRunner;
import org.quickload.TestRuntimeBinder;
import org.quickload.TestUtilityModule;
import org.quickload.buffer.Buffer;
import org.quickload.channel.FileBufferChannel;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageChannel;
import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.plugin.MockPluginSource;
import org.quickload.record.PageAllocator;
import org.quickload.record.PageBuilder;
import org.quickload.record.Schema;
import org.quickload.spi.FileInputPlugin.InputTask;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class TestBasicParserPlugin
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private static class TestTargetBasicParserPlugin extends BasicParserPlugin
    {
        boolean emulateException = false;

        @Override
        public TaskSource getBasicParserTask(ExecTask exec, ConfigSource config)
        {
            return new TaskSource();
        }

        @Override
        public void runBasicParser(ExecTask exec, TaskSource taskSource,
                int processorIndex, FileBufferInput fileBufferInput,
                PageOutput pageOutput)
        {
            PageAllocator pageAllocator = exec.getPageAllocator();
            Schema schema = exec.getSchema();
            MockRecordWriter recordWriter = new MockRecordWriter();
            while (fileBufferInput.nextFile()) {
                for (Buffer buffer : fileBufferInput) {
                    // consume all inputstream
                }
            }
            // then writes something
            try (PageBuilder builder = new PageBuilder(pageAllocator, schema,
                    pageOutput)) {
                builder.addRecord(recordWriter);
                System.err.println(".");
                if (emulateException) {
                    throw new RuntimeException("emulated exception");
                }
            }
        }
    }

    @Test
    public void testRunParser()
    {
        // force using TestTargetFileEncoderPlugin
        MockDecoderPlugin decoderPlugin = new MockDecoderPlugin();
        binder.addModule(MockPluginSource.newInjectModule(
                FileDecoderPlugin.class, decoderPlugin));

        TestTargetBasicParserPlugin plugin = new TestTargetBasicParserPlugin();

        Schema schema = newSchema(newColumn("c1", BOOLEAN),
                newColumn("c2", LONG), newColumn("c3", DOUBLE),
                newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
        ExecTask exec = binder.newExecTask();
        exec.setSchema(schema);

        PageChannel pageOutput = new PageChannel(Integer.MAX_VALUE);
        try {
            // prepare pages
            FileBufferChannel fileBuffer = prepareFileBufferChannel(exec);
            TaskSource task = prepareParserTask(exec, plugin);

            // run plugin
            plugin.runParser(exec, task, 0, fileBuffer.getInput(),
                    pageOutput.getOutput());
            fileBuffer.close();
            // test
            // assertEquals(3, plugin.records.size());
            // assertEquals(5, plugin.records.get(0).size());
            // // encoded to 3 buffers in 1 file.
            // assertEquals(1, encoderPlugin.getFiles().size());
            // assertEquals(3, encoderPlugin.getFiles().get(0).size());
        } finally {
            pageOutput.close();
        }
    }

    private FileBufferChannel prepareFileBufferChannel(ExecTask exec)
    {
        FileBufferChannel channel = exec.newFileBufferChannel();
        channel.getOutput().addFile();
        channel.completeProducer();
        return channel;
    }

    private TaskSource prepareParserTask(ExecTask exec,
            TestTargetBasicParserPlugin plugin)
    {
        // Apply same encoder twice for testing.
        ConfigSource config = new ConfigSource().set(
                "parser",
                new ConfigSource().setString("type", "dummy").set(
                        "file_decoders",
                        ConfigSource
                                .arrayNode()
                                .add(ConfigSource.objectNode()
                                        .put("type", "dummy2")
                                        .put("postfix", "nahi"))
                                .add(ConfigSource.objectNode()
                                        .put("type", "dummy3")
                                        .put("postfix", "nahi"))));
        InputTask inputTask = exec.loadConfig(config, InputTask.class);
        inputTask.setParserTask(plugin.getParserTask(exec,
                inputTask.getParserConfig()));
        return inputTask.getParserTask();
    }
}
