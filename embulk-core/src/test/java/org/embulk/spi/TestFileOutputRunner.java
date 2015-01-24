package org.embulk.spi;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.embulk.EmbulkTestRuntime;
import org.embulk.config.CommitReport;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.time.Timestamp;
import org.embulk.type.Schema;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestFileOutputRunner
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    public interface PluginTask extends Task
    {
    }

    private static class MockFileOutputPlugin implements FileOutputPlugin
    {
        Boolean transactionCompleted = null;

        @Override
        public NextConfig transaction(ConfigSource config, int processorCount,
                FileOutputPlugin.Control control)
        {
            PluginTask task = config.loadConfig(PluginTask.class);
            control.run(task.dump());
            return Exec.newNextConfig();
        }

        @Override
        public TransactionalFileOutput open(TaskSource taskSource,
                final int processorIndex)
        {
            return new TransactionalFileOutput()
            {

                @Override
                public void nextFile()
                {
                }

                @Override
                public void add(Buffer buffer)
                {
                }

                @Override
                public void finish()
                {
                }

                @Override
                public void close()
                {
                }

                @Override
                public void abort()
                {
                    transactionCompleted = false;
                }

                @Override
                public CommitReport commit()
                {
                    transactionCompleted = true;
                    return Exec.newCommitReport();
                }
            };
        }
    }

    @Test
    public void testMockFormatterIteration()
    {
        MockFileOutputPlugin fileOutputPlugin = new MockFileOutputPlugin();
        final FileOutputRunner runner = new FileOutputRunner(fileOutputPlugin);

        ImmutableList<ImmutableMap<String, String>> columns = ImmutableList.of(
                ImmutableMap.of("name", "col1", "type", "boolean"),
                ImmutableMap.of("name", "col2", "type", "long"),
                ImmutableMap.of("name", "col3", "type", "double"),
                ImmutableMap.of("name", "col4", "type", "string"),
                ImmutableMap.of("name", "col5", "type", "timestamp"));
        ConfigSource config = Exec
                .newConfigSource()
                .set("type", "unused?")
                .set("formatter",
                        ImmutableMap.of("type", "mock", "columns", columns));
        final Schema schema = config.getNested("formatter")
                .loadConfig(MockParserPlugin.PluginTask.class)
                .getSchemaConfig().toSchema();

        runner.transaction(config, schema, 1, new OutputPlugin.Control()
        {
            public List<CommitReport> run(final TaskSource outputTask)
            {
                TransactionalPageOutput tran = runner.open(outputTask, schema,
                        1);
                boolean committed = false;
                try {
                    for (Page page : PageTestUtils.buildPage(
                            runtime.getBufferAllocator(), schema, true, 2L,
                            3.0D, "45", Timestamp.ofEpochMilli(678L), true, 2L,
                            3.0D, "45", Timestamp.ofEpochMilli(678L))) {
                        tran.add(page);
                    }
                    tran.commit();
                    committed = true;
                } finally {
                    if (!committed) {
                        tran.abort();
                    }
                    tran.close();
                }
                return new ArrayList<CommitReport>();
            }
        });

        assertEquals(true, fileOutputPlugin.transactionCompleted);
        assertEquals(2, MockFormatterPlugin.records.size());
        for (List<Object> record : MockFormatterPlugin.records) {
            assertEquals(Boolean.TRUE, record.get(0));
            assertEquals(2L, record.get(1));
            assertEquals(3.0D, (Double) record.get(2), 0.1D);
            assertEquals("45", record.get(3));
            assertEquals(678L, ((Timestamp) record.get(4)).toEpochMilli());
        }
    }

    @Test
    public void testTransactionAborted()
    {
        MockFileOutputPlugin fileOutputPlugin = new MockFileOutputPlugin();
        final FileOutputRunner runner = new FileOutputRunner(fileOutputPlugin);

        ImmutableList<ImmutableMap<String, String>> columns = ImmutableList.of(
                ImmutableMap.of("name", "col1", "type", "boolean"),
                ImmutableMap.of("name", "col2", "type", "long"),
                ImmutableMap.of("name", "col3", "type", "double"),
                ImmutableMap.of("name", "col4", "type", "string"),
                ImmutableMap.of("name", "col5", "type", "timestamp"));
        ConfigSource config = Exec
                .newConfigSource()
                .set("type", "unused?")
                .set("formatter",
                        ImmutableMap.of("type", "mock", "columns", columns));
        final Schema schema = config.getNested("formatter")
                .loadConfig(MockParserPlugin.PluginTask.class)
                .getSchemaConfig().toSchema();

        try {
            runner.transaction(config, schema, 1, new OutputPlugin.Control()
            {
                public List<CommitReport> run(final TaskSource outputTask)
                {
                    TransactionalPageOutput tran = runner.open(outputTask,
                            schema, 1);
                    boolean committed = false;
                    try {
                        tran.add(null);
                        tran.commit();
                        committed = true;
                    } finally {
                        if (!committed) {
                            tran.abort();
                        }
                        tran.close();
                    }
                    return new ArrayList<CommitReport>();
                }
            });
        } catch (NullPointerException npe) {
        }

        assertEquals(false, fileOutputPlugin.transactionCompleted);
    }
}