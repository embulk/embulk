package org.quickload.spi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.junit.Before;
import org.junit.Test;
import org.quickload.TestUtilityModule;
import org.quickload.channel.PageInput;
import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.ModelManager;
import org.quickload.config.NullReport;
import org.quickload.config.Report;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.exec.BufferManager;
import org.quickload.exec.ExecModule;
import org.quickload.exec.ExtensionServiceLoaderModule;
import org.quickload.exec.LocalExecutor;
import org.quickload.plugin.BuiltinPluginSourceModule;
import org.quickload.plugin.InjectedPluginSource;
import org.quickload.record.Column;
import org.quickload.record.DoubleType;
import org.quickload.record.LongType;
import org.quickload.record.Page;
import org.quickload.record.PageBuilder;
import org.quickload.record.PageReader;
import org.quickload.record.RandomManager;
import org.quickload.record.RandomRecordGenerator;
import org.quickload.record.RandomSchemaGenerator;
import org.quickload.record.Record;
import org.quickload.record.RecordConsumer;
import org.quickload.record.RecordCursor;
import org.quickload.record.RecordProducer;
import org.quickload.record.Schema;
import org.quickload.record.StringType;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InputOutputPluginComboTest {

    static class InputPluginMock implements InputPlugin
    {
        public interface PluginTask
                extends Task
        {
        }

        private static List<Record> expected;

        private ModelManager modelManager;
        private RandomRecordGenerator recordGen;
        private RandomSchemaGenerator schemaGen;

        @Inject
        public InputPluginMock(ModelManager modelManager,
                RandomRecordGenerator recordGen, RandomSchemaGenerator schemaGen)
        {
            this.modelManager = modelManager;
            this.recordGen = recordGen;
            this.schemaGen = schemaGen;
        }

        @Override
        public TaskSource getInputTask(ProcTask proc, ConfigSource config) {
            PluginTask task = config.loadModel(modelManager, PluginTask.class);
            proc.setProcessorCount(1);
            proc.setSchema(schemaGen.generate(60));
            return config.dumpTask(task);
        }

        @Override
        public void runInputTransaction(ProcTask proc, TaskSource taskSource,
                ProcControl control)
        {
            control.run();
        }

        @Override
        public Report runInput(ProcTask proc, TaskSource taskSource,
                int processorIndex, PageOutput pageOutput)
        {
            Schema schema = proc.getSchema();
            expected = ImmutableList.copyOf(recordGen.generate(schema, 5000));
            PageBuilder builder = new PageBuilder(proc.getPageAllocator(), schema, pageOutput);
            for (final Record record : expected) {
                schema.produce(builder, new RecordProducer()
                {
                    @Override
                    public void setLong(Column column, LongType.Setter setter)
                    {
                        setter.setLong((Long) record.getObject(column.getIndex()));
                    }

                    @Override
                    public void setDouble(Column column, DoubleType.Setter setter)
                    {
                        setter.setDouble((Double) record.getObject(column.getIndex()));
                    }

                    @Override
                    public void setString(Column column, StringType.Setter setter)
                    {
                        setter.setString((String) record.getObject(column.getIndex()));
                    }
                });
                builder.addRecord();
            }
            builder.flush();
            return new NullReport();
        }
    }

    static class OutputPluginMock implements OutputPlugin
    {
        public interface PluginTask
                extends Task
        {
        }

        private static List<Record> actual;

        private ModelManager modelManager;

        @Inject
        public OutputPluginMock(ModelManager modelManager)
        {
            this.modelManager = modelManager;
        }

        @Override
        public TaskSource getOutputTask(ProcTask proc, ConfigSource config)
        {
            proc.setProcessorCount(1);

            PluginTask task = config.loadModel(modelManager, PluginTask.class);
            return config.dumpTask(task);
        }

        @Override
        public void runOutputTransaction(ProcTask proc, TaskSource taskSource,
                                         ProcControl control)
        {
            control.run();
        }

        @Override
        public Report runOutput(ProcTask proc, TaskSource taskSource,
                                int processorIndex, PageInput pageInput)
        {
            Schema schema = proc.getSchema();
            actual = new ArrayList<Record>();
            PageReader reader = new PageReader(schema);
            for (Page page : pageInput) {
                try (RecordCursor cursor = reader.cursor(page)) {
                    while (cursor.next()) {
                        final Object[] values = new Object[schema.getColumns().size()];
                        schema.consume(cursor, new RecordConsumer()
                        {
                            @Override
                            public void setNull(Column column)
                            {
                                // TODO
                            }

                            @Override
                            public void setLong(Column column, long value)
                            {
                                values[column.getIndex()] = value;
                            }

                            @Override
                            public void setDouble(Column column, double value)
                            {
                                values[column.getIndex()] = value;
                            }

                            @Override
                            public void setString(Column column, String value)
                            {
                                values[column.getIndex()] = value;
                            }
                        });
                        actual.add(new Record(values));
                    }
                }
            }
            return new NullReport();
        }
    }

    private Injector injector;
    private ModelManager modelManager;

    @Before
    public void buildModules()
    {
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new ExecModule());
        modules.add(new ExtensionServiceLoaderModule());
        modules.add(new BuiltinPluginSourceModule());
        modules.add(new TestUtilityModule());
        modules.add(new Module()
        {
            @Override
            public void configure(Binder binder)
            {
                binder.bind(InputPlugin.class).annotatedWith(Names.named("mock")).to(InputPluginMock.class);
                binder.bind(OutputPlugin.class).annotatedWith(Names.named("mock")).to(OutputPluginMock.class);
            }
        });
        injector = Guice.createInjector(modules.build());
        modelManager = injector.getInstance(ModelManager.class);
    }

    @Test
    public void test() throws Exception
    {
        JsonNodeFactory js = JsonNodeFactory.instance;
        ObjectNode json = js.objectNode();

        ObjectNode inputType = js.objectNode();
        inputType.put("injected", "mock");
        json.put("in:type", inputType);

        ObjectNode outputType = js.objectNode();
        outputType.put("injected", "mock");
        json.put("out:type", outputType);

        ConfigSource config = new ConfigSource(modelManager, json);
        LocalExecutor exec = injector.getInstance(LocalExecutor.class);
        exec.configure(config);
        exec.run();

        assertEquals(InputPluginMock.expected, OutputPluginMock.actual);
    }

    private static ObjectNode column(JsonNodeFactory js,
                                     int index, String name, String type)
    {
        ObjectNode column = js.objectNode();
        column.put("index", index);
        column.put("name", name);
        column.put("type", type);
        return column;
    }
}
