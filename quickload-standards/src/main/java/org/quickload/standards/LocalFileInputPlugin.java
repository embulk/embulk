package org.quickload.standards;

import javax.validation.constraints.NotNull;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.exec.BufferManager;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class LocalFileInputPlugin
        extends FileInputPlugin<LocalFileInputPlugin.Task>
{
    @Inject
    public LocalFileInputPlugin(PluginManager pluginManager) {
        super(pluginManager);
    }

    // TODO consider when the page allocator object is released?

    public interface Task
            extends FileInputTask
    {
        @Config("in:paths") // TODO temporarily added 'in:'
        @NotNull
        public List<String> getPaths();
    }

    @Override
    public Task getFileInputTask(ConfigSource config, ParserTask parserTask)
    {
        Task task = config.load(Task.class);
        task.setProcessorCount(task.getPaths().size());
        task.setSchema(parserTask.getSchema());
        task.setParserTask(config.dumpTask(parserTask));
        task.validate();
        return task;
    }

    @Override
    public InputProcessor startFileInputProcessor(final Task task,
            final int processorIndex, final BufferOperator op)
    {
        return ThreadInputProcessor.start(op, new Function<BufferOperator, ReportBuilder>() {
            public ReportBuilder apply(BufferOperator op) {
                return readFile(task, processorIndex, op);
            }
        });
    }

    public static ReportBuilder readFile(Task task, int processorIndex, BufferOperator op)
    {
        // TODO ad-hoc
        String path = task.getPaths().get(processorIndex);

        try {
            File file = new File(path);
            byte[] bytes = new byte[(int) file.length()]; // TODO ad-hoc

            int len, offset = 0;
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                while ((len = in.read(bytes, offset, bytes.length - offset)) > 0) {
                    offset += len;
                }
                Buffer buffer = new Buffer(bytes);
                op.addBuffer(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }

        return DynamicReport.builder(); // TODO
    }
}
