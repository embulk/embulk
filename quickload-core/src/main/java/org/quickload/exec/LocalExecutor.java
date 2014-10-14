package org.quickload.exec;

import java.util.List;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.base.Function;
import com.google.inject.Inject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.ProcConfig;
import org.quickload.spi.ProcTask;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.InputTransaction;
import org.quickload.spi.OutputTransaction;
import org.quickload.spi.InputProcessor;
import org.quickload.spi.PageOperator;
import org.quickload.spi.Report;

public class LocalExecutor
        implements AutoCloseable
{
    private final PluginManager pluginManager;

    private ConfigSource config;
    private InputPlugin in;
    private OutputPlugin out;

    private InputTransaction inputTran;
    private OutputTransaction outputTran;

    private int processorCount;
    private TaskSource inputTask;
    private TaskSource outputTask;
    private ProcTask procTask;

    private final List<ProcessingUnit> units = new ArrayList<ProcessingUnit>();

    public interface ExecutorTask
            extends Task
    {
        // TODO
        @Config("in:type")
        @NotNull
        public JsonNode getInputType();

        // TODO
        @Config("out:type")
        @NotNull
        public JsonNode getOutputType();

        // TODO setProcTask
        public int getProcessorCount();
        public void setProcessorCount(int processorCount);

        public Schema getSchema();
        public void setSchema(Schema schema);
    }

    @Inject
    public LocalExecutor(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    protected InputPlugin newInputPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(InputPlugin.class, typeConfig);
    }

    protected OutputPlugin newOutputPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(OutputPlugin.class, typeConfig);
    }

    public void configure(ConfigSource config)
    {
        this.config = config;
        ExecutorTask task = config.loadTask(ExecutorTask.class);

        in = newInputPlugin(task.getInputType());
        out = newOutputPlugin(task.getOutputType());
        inputTran = in.newInputTransaction();
        outputTran = out.newOutputTransaction();

        ProcConfig proc = new ProcConfig();
        inputTask = inputTran.getInputTask(proc, config);
        procTask = proc.getProcTask();
        outputTask = outputTran.getOutputTask(procTask, config);

        task.setProcessorCount(procTask.getProcessorCount());
        task.setSchema(procTask.getSchema());

        validateTransaction();
    }

    private void validateTransaction()
    {
        // TODO
        System.out.println("input: "+inputTask);
        System.out.println("output: "+outputTask);
    }

    public void begin()
    {
        inputTran.begin();
        outputTran.begin();
    }

    private static class ProcessingUnit
    {
        private final InputProcessor inputProc;
        private final PageOperator outputOp;
        private final MonitoringOperator monitorOp;
        private Report inputReport;
        private Report outputReport;

        public ProcessingUnit(InputProcessor inputProc, PageOperator outputOp,
                MonitoringOperator monitorOp)
        {
            this.inputProc = inputProc;
            this.outputOp = outputOp;
            this.monitorOp = monitorOp;
        }

        public void join() throws InterruptedException
        {
            this.inputReport = inputProc.join();
            this.outputReport = monitorOp.getReport();
        }

        public Report getInputReport()
        {
            return inputReport;
        }

        public Report getOutputReport()
        {
            return outputReport;
        }

        public static Function<ProcessingUnit, Report> inputReportGetter()
        {
            return new Function<ProcessingUnit, Report>() {
                public Report apply(ProcessingUnit unit)
                {
                    return unit.getInputReport();
                }
            };
        }

        public static Function<ProcessingUnit, Report> outputReportGetter()
        {
            return new Function<ProcessingUnit, Report>() {
                public Report apply(ProcessingUnit unit)
                {
                    return unit.getOutputReport();
                }
            };
        }

        public void close() throws Exception
        {
            inputProc.close();
        }
    }

    public void start()
    {
        for (int procIndex=0; procIndex < procTask.getProcessorCount(); procIndex++) {
            PageOperator outputOp = out.openPageOperator(procTask, outputTask, procIndex);
            try {
                MonitoringOperator monitorOp = new MonitoringOperator(outputOp);
                InputProcessor inputProc = in.startInputProcessor(procTask, inputTask, procIndex, monitorOp);
                units.add(new ProcessingUnit(inputProc, outputOp, monitorOp));
            } catch (RuntimeException ex) {
                try {
                    outputOp.close();
                } catch (Exception suppressed) {
                    ex.addSuppressed(suppressed);
                }
                throw ex;
            }
        }

        // TODO start progress reporter thread
    }

    public void join() throws InterruptedException
    {
        for (ProcessingUnit unit : units) {
            unit.join();
        }
    }

    public void abort()
    {
        // TODO async thread safe method

        outputTran.abort();
        inputTran.abort();
    }

    public void commit()
    {
        // TODO check join() was already called

        List<Report> inputReports = ImmutableList.copyOf(
                Iterables.transform(units, ProcessingUnit.inputReportGetter()));
        List<Report> outputReports = ImmutableList.copyOf(
                Iterables.transform(units, ProcessingUnit.outputReportGetter()));

        outputTran.commit(outputReports);
        inputTran.commit(inputReports);
    }

    @Override
    public void close() throws Exception
    {
        for (ProcessingUnit unit : units) {
            unit.close();
        }
    }
}
