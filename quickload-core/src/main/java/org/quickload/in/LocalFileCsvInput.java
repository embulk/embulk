package org.quickload.in;

import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import org.quickload.exec.BufferManager;
import org.quickload.record.Page;
import org.quickload.record.PageBuilder;
import org.quickload.record.RecordBuilder;
import org.quickload.record.Schema;
import org.quickload.spi.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

public class LocalFileCsvInput
        extends BasicInputPlugin<LocalFileCsvInput.Task>
{
    public interface Task
            extends InputTask, DynamicModel<Task>
    {
        @Config("paths")
        public List<String> getPaths();
    }

    public static class Processor
            extends ThreadInputProcessor
    {
        private final Task task;
        private final int processorIndex;
        private final BufferManager bufferManager;

        public Processor(Task task,
                int processorIndex, OutputOperator op)
        {
            super(op);
            this.task = task;
            this.processorIndex = processorIndex;
            this.thread.start(); // TODO when can we start the thread??
            this.bufferManager = new BufferManager();
        }

        @Override
        public ReportBuilder runThread() throws Exception
        {
            String path = task.getPaths().get(processorIndex); // TODO ad hoc
            System.out.println("## path: " + path);
            Schema schema = task.getSchema(); // TODO doesn't work
            System.out.println("## schema: " + schema);

            // TODO simple implementation

            PageBuilder pageBuilder = new PageBuilder(bufferManager, schema, op);
            RecordBuilder recordBuilder = pageBuilder.builder();
            recordBuilder.
            Page page;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
                String line;
                int pos = 0;
                page = Page.allocate(4096); // TODO use Page allocator or page builder?
                while ((line = r.readLine()) != null) {
                    System.out.println("# line: " + line);
                    // TODO validate column size
                    String[] columns = line.split(",");
                    for (String column : columns) {
                        byte[] bytes = column.getBytes();
                        // TODO int:setBytes??
                        page.setBytes(pos, bytes); // TODO charset?
                        pos += bytes.length;
                    }
                }
            }
            if (page != null) {
                try {
                    op.addPage(page);
                } finally {
                    page.clear();
                }
            }

            return DynamicReport.builder();
        }

        @Override
        public InputProgress getProgress()
        {
            return null;
        }
    }

    @Override
    public Task getTask(ConfigSource config)
    {
        Task task = config.load(Task.class);
        //task.getBasePath()
        //task.set("paths", ...);
        //return task.validate();
        task.set("ProcessorCount", task.getPaths().size());
        return task.validate();
    }

    @Override
    public Processor startProcessor(Task task,
            int processorIndex, OutputOperator op)
    {
        return new Processor(task, processorIndex, op);
    }
}
