package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.embulk.record.PageTestUtils.newColumn;
import static org.embulk.record.PageTestUtils.newSchema;
import static org.embulk.record.Types.BOOLEAN;
import static org.embulk.record.Types.DOUBLE;
import static org.embulk.record.Types.LONG;
import static org.embulk.record.Types.STRING;
import static org.embulk.record.Types.TIMESTAMP;

import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.embulk.GuiceJUnitRunner;
import org.embulk.TestRuntimeBinder;
import org.embulk.TestUtilityModule;
import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferChannel;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageChannel;
import org.embulk.channel.PageOutput;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.plugin.MockPluginSource;
import org.embulk.record.Page;
import org.embulk.record.PageAllocator;
import org.embulk.record.PageBuilder;
import org.embulk.record.Schema;
import org.embulk.spi.FileInputPlugin.InputTask;

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
                    buffer.get();
                }
            }
            // then writes something
            try (PageBuilder builder = new PageBuilder(pageAllocator, schema,
                    pageOutput)) {
                builder.addRecord(recordWriter);
                builder.addRecord(recordWriter);
                builder.addRecord(recordWriter);
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
            TaskSource task = prepareParserTask(exec, plugin);
            // prepare pages
            try (FileBufferChannel fileBuffer = prepareFileBufferChannel(exec)) {
                // run plugin
                plugin.runParser(exec, task, 0, fileBuffer.getInput(),
                        pageOutput.getOutput());
                pageOutput.completeProducer();
                fileBuffer.completeConsumer();
            }
            Iterator<Page> ite = pageOutput.getInput().iterator();
            assertTrue(ite.hasNext());
            assertEquals(3, ite.next().getRecordCount());
            assertFalse(ite.hasNext());
        } finally {
            pageOutput.close();
        }
    }

    @Test(expected = RuntimeException.class)
    public void testRunParserPropagatesException()
    {
        // force using TestTargetFileEncoderPlugin
        MockDecoderPlugin decoderPlugin = new MockDecoderPlugin();
        binder.addModule(MockPluginSource.newInjectModule(
                FileDecoderPlugin.class, decoderPlugin));

        TestTargetBasicParserPlugin plugin = new TestTargetBasicParserPlugin();
        plugin.emulateException = true;

        Schema schema = newSchema(newColumn("c1", BOOLEAN),
                newColumn("c2", LONG), newColumn("c3", DOUBLE),
                newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
        ExecTask exec = binder.newExecTask();
        exec.setSchema(schema);

        PageChannel pageOutput = new PageChannel(Integer.MAX_VALUE);
        try {
            TaskSource task = prepareParserTask(exec, plugin);
            // prepare pages
            try (FileBufferChannel fileBuffer = prepareFileBufferChannel(exec)) {
                // run plugin
                plugin.runParser(exec, task, 0, fileBuffer.getInput(),
                        pageOutput.getOutput());
                fail();
            }
        } finally {
            pageOutput.close();
        }
    }

    private FileBufferChannel prepareFileBufferChannel(ExecTask exec)
    {
        FileBufferChannel channel = exec.newFileBufferChannel();
        channel.getOutput().addFile();
        channel.getOutput().addFile();
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
