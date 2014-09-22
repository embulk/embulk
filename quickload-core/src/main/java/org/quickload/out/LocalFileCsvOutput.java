package org.quickload.out;

import java.util.List;

import org.quickload.config.ConfigSource;
import org.quickload.record.Page;
import org.quickload.spi.BasicOutputPlugin;
import org.quickload.spi.InputTask;
import org.quickload.spi.OutputTask;
import org.quickload.spi.Report;
import org.quickload.spi.DynamicReport;
import org.quickload.spi.AbstractOutputOperator;

public class LocalFileCsvOutput
        extends BasicOutputPlugin<LocalFileCsvOutput.Task>
{
    // TODO use DynamicTask
    public static class Task
            implements OutputTask
    {
        private List<String> paths;

        public List<String> getPaths()
        {
            return paths;
        }
    }

    public static class Operator
            extends AbstractOutputOperator
    {
        @Override
        public void addPage(Page page)
        {
            // TODO
        }

        @Override
        public void close() throws Exception
        {
        }

        @Override
        public Report completed()
        {
            return DynamicReport.builder().build(null);
        }
    }

    @Override
    public Task getTask(ConfigSource config, InputTask input)
    {
        return new Task();
    }

    @Override
    public void begin(Task task)
    {
    }

    @Override
    public Operator openOperator(Task task, int processorIndex)
    {
        return new Operator();
    }

    @Override
    public void commit(Task task, List<Report> reports)
    {
    }

    @Override
    public void abort(Task task)
    {
    }
}
