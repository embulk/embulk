package org.quickload.spi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.inject.*;
import com.google.inject.name.Names;
import org.junit.Before;
import org.junit.Test;
import org.quickload.TestUtilityModule;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.channel.*;
import org.quickload.config.*;
import org.quickload.exec.BufferManager;
import org.quickload.exec.ExecModule;
import org.quickload.exec.ExtensionServiceLoaderModule;
import org.quickload.exec.LocalExecutor;
import org.quickload.plugin.BuiltinPluginSourceModule;
import org.quickload.record.*;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InputFormattingOutputPluginComboTest {

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
            proc.setSchema(schemaGen.generate(2));
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
            expected = ImmutableList.copyOf(recordGen.generate(schema, 2));
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

    static class FormattingPluginMock implements FormatterPlugin
    {
        public interface PluginTask
                extends Task
        {
        }

        private ModelManager modelManager;

        @Inject
        public FormattingPluginMock(ModelManager modelManager)
        {
            this.modelManager = modelManager;
        }

        @Override
        public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
        {
            PluginTask task = config.loadTask(PluginTask.class);
            return config.dumpTask(task);
        }

        @Override
        public void runFormatter(ProcTask proc,
                TaskSource taskSource, int processorIndex,
                PageInput pageInput, BufferOutput bufferOutput)
        {
            PageReader pageReader = new PageReader(proc.getSchema());
            Schema schema = proc.getSchema();
            BufferAllocator bufferAllocator = proc.getBufferAllocator();

            List<Object[]> actual = new ArrayList<Object[]>();
            for (Page page : pageInput) {
                RecordCursor cursor = pageReader.cursor(page);

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
                    actual.add(values);
                }

                String json = modelManager.writeJson(actual);

                byte[] bytes = json.getBytes();
                Buffer buf = proc.getBufferAllocator().allocateBuffer(bytes.length);
                buf.write(bytes, 0, bytes.length);
                bufferOutput.add(buf);
            }
        }
    }

    static class FormattingOutputPluginMock implements OutputPlugin
    {
        private static List<Record> actual;

        private ModelManager modelManager;

        @Inject
        public FormattingOutputPluginMock(ModelManager modelManager)
        {
            this.modelManager = modelManager;
        }

        public TaskSource getFormattingOutputTask(ProcTask proc, ConfigSource config)
        {
            PluginTask task = config.loadModel(modelManager, PluginTask.class);
            return config.dumpTask(task);
        }

        @Override
        public void runOutputTransaction(ProcTask proc, TaskSource taskSource,
                ProcControl control)
        {
            control.run();
        }

        public Report runFormattingOutput(ProcTask proc,
                TaskSource taskSource, int processorIndex,
                BufferInput bufferInput)
        {
            try {
                for (Buffer buffer : bufferInput) {
                    ByteBuffer buf = buffer.getBuffer();
                    byte[] bytes = buf.array(); // TODO

                    List<Object[]> act = new ObjectMapper().readValue(new String(bytes), new TypeReference<List<Object[]>>() {});
                    actual = new ArrayList<Record>(act.size());
                    for (Object[] vs : act) {
                        actual.add(new Record(vs));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new NullReport();
        }

        public interface PluginTask
                extends Task
        {
            @Config("out:formatter_type")
            @NotNull
            public JsonNode getFormatterType();

            public TaskSource getFormatterTask();
            public void setFormatterTask(TaskSource task);

            public TaskSource getFormattingOutputTask();
            public void setFormattingOutputTask(TaskSource task);
        }


        @Override
        public TaskSource getOutputTask(ProcTask proc, ConfigSource config)
        {
            proc.setProcessorCount(1);

            PluginTask task = config.loadModel(modelManager, PluginTask.class);
            FormatterPlugin formatter = proc.newPlugin(FormatterPlugin.class, task.getFormatterType());
            task.setFormatterTask(formatter.getFormatterTask(proc, config));
            task.setFormattingOutputTask(getFormattingOutputTask(proc, config));
            return config.dumpTask(task);
        }

        @Override
        public Report runOutput(final ProcTask proc,
                final TaskSource taskSource, final int processorIndex,
                final PageInput pageInput)
        {
            final PluginTask task = taskSource.loadTask(PluginTask.class);
            final FormatterPlugin formatter = proc.newPlugin(FormatterPlugin.class, task.getFormatterType());
            try (final BufferChannel channel = proc.newBufferChannel()) {
                proc.startPluginThread(new PluginThread() {
                    public void run()
                    {
                        try {
                            formatter.runFormatter(proc,
                                    task.getFormatterTask(), processorIndex,
                                    pageInput, channel.getOutput());
                        } finally {
                            channel.completeConsumer();
                        }
                    }
                });

                Report report = runFormattingOutput(proc,
                        task.getFormattingOutputTask(), processorIndex,
                        channel.getInput());
                channel.completeProducer();
                channel.join();

                return report;
            }
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
                binder.bind(OutputPlugin.class).annotatedWith(Names.named("mock")).to(FormattingOutputPluginMock.class);
                binder.bind(FormatterPlugin.class).annotatedWith(Names.named("mock")).to(FormattingPluginMock.class);
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

        ObjectNode formatterType = js.objectNode();
        formatterType.put("injected", "mock");
        json.put("out:formatter_type", formatterType);

        ObjectNode outputType = js.objectNode();
        outputType.put("injected", "mock");
        json.put("out:type", outputType);

        ConfigSource config = new ConfigSource(modelManager, json);
        LocalExecutor exec = injector.getInstance(LocalExecutor.class);
        exec.configure(config);
        exec.run();

        assertEquals(InputPluginMock.expected, FormattingOutputPluginMock.actual);
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
