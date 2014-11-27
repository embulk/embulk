package org.quickload.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quickload.GuiceJUnitRunner;
import org.quickload.TestRuntimeBinder;
import org.quickload.TestUtilityModule;
import org.quickload.channel.FileBufferOutput;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.plugin.MockPluginSource;
import org.quickload.record.RandomRecordGenerator;
import org.quickload.record.RandomSchemaGenerator;
import org.quickload.record.Schema;
import org.quickload.spi.FileInputPlugin.InputTask;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class TestFileInputPlugin
{
    @Inject
    protected RandomSchemaGenerator schemaGen;
    @Inject
    protected RandomRecordGenerator recordGen;

    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private static interface TestParserTask
            extends Task
    {
        @Config("field")
        public String getField();
    }
    
    private static class ParserTestFileInputPlugin
            extends FileInputPlugin
    {
        public NextConfig runFileInputTransaction(ExecTask exec,
                ConfigSource config, ExecControl control)
        {
            control.run(new TaskSource());
            return new NextConfig();
        }

        public Report runFileInput(ExecTask exec, TaskSource taskSource,
                int processorIndex, FileBufferOutput fileBufferOutput)
        {
            return new Report();
        }
    }

    private static class TestTargetFileInputPlugin extends FileInputPlugin
    {
        boolean emulateException = false;
        
        @Override
        public NextConfig runFileInputTransaction(ExecTask exec,
                ConfigSource config, ExecControl control)
        {
            // Not called
            return new NextConfig();
        }

        public Report runFileInput(ExecTask exec, TaskSource taskSource,
                int processorIndex, FileBufferOutput fileBufferOutput)
        {
            if (emulateException) {
                throw new RuntimeException("emulated exception");
            }
            return new Report();
        }
    }

    @Test
    public void testTransactionSetsParserTask()
    {
        Schema schema = schemaGen.generate(0);
        MockParserPlugin parser = new MockParserPlugin(
                schema, ImmutableList.copyOf(recordGen.generate(schema, 0)),
                TestParserTask.class);
        binder.addModule(MockPluginSource.newInjectModule(ParserPlugin.class, parser));

        ExecTask exec = binder.newExecTask();

        ConfigSource config = new ConfigSource()
            .set("parser",
                    new ConfigSource()
                        .setString("field", "frsyuki")
                        .setString("type", "dummy"));

        ParserTestFileInputPlugin plugin = binder.getInstance(ParserTestFileInputPlugin.class);

        MockExecControl control = new MockExecControl();
        plugin.runInputTransaction(exec, config, control);

        FileInputPlugin.InputTask inputTask =
            exec.loadTask(control.getTaskSource(), FileInputPlugin.InputTask.class);
        TestParserTask parserTask =
            exec.loadTask(inputTask.getParserTask(), TestParserTask.class);

        assertEquals("frsyuki", parserTask.getField());
    }

    @Test
    public void testRunInput()
    {
        prepareMockParser();
        ExecTask exec = binder.newExecTask();
        InputTask task = prepareInputTask(exec);

        TestTargetFileInputPlugin plugin = binder
                .getInstance(TestTargetFileInputPlugin.class);
        MockPageOutput pageOutput = new MockPageOutput();
        plugin.runInput(exec, exec.dumpTask(task), 0, pageOutput);
        // actually the size depends on Page's buffer size (BufferManager
        // decides)
        assertEquals(1, pageOutput.getPages().size());
        assertEquals(100, pageOutput.getPages().get(0).getRecordCount());
    }

    @Test(expected = RuntimeException.class)
    public void testRunInputPropagatesException()
    {
        prepareMockParser();
        ExecTask exec = binder.newExecTask();
        InputTask task = prepareInputTask(exec);

        TestTargetFileInputPlugin plugin = binder
                .getInstance(TestTargetFileInputPlugin.class);
        plugin.emulateException = true;
        plugin.runInput(exec, exec.dumpTask(task), 0, null);
    }

    private void prepareMockParser()
    {
        // prepare 100 records auto-generate parser plugin
        Schema schema = schemaGen.generate(4);
        MockParserPlugin parser = new MockParserPlugin(schema,
                ImmutableList.copyOf(recordGen.generate(schema, 100)),
                TestParserTask.class);
        // always use MockParserPlugin as "parser" plugin
        binder.addModule(MockPluginSource.newInjectModule(ParserPlugin.class,
                parser));
    }

    private InputTask prepareInputTask(ExecTask exec)
    {
        ConfigSource config = new ConfigSource().set("parser",
                new ConfigSource().setString("type", "dummy"));
        InputTask task = exec.loadConfig(config, InputTask.class);
        task.setParserTask(new TaskSource());
        task.setFileInputTask(new TaskSource());
        return task;
    }
}
