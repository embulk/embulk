package org.quickload.exec;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.base.Function;
import com.google.inject.Inject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.model.ModelManager;
import org.quickload.plugin.PluginManager;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.InputTransaction;
import org.quickload.spi.OutputTransaction;
import org.quickload.spi.Task;
import org.quickload.spi.InputTask;
import org.quickload.spi.OutputTask;
import org.quickload.spi.InputProcessor;
import org.quickload.spi.OutputOperator;
import org.quickload.spi.Report;

public class LocalExecutor
        implements AutoCloseable
{
    private final ModelManager modelManager;
    private final PluginManager pluginManager;

    private ConfigSource config;
    private InputPlugin in;
    private OutputPlugin out;

    private InputTransaction inputTran;
    private OutputTransaction outputTran;

    private int processorCount;
    private TaskSource inputTaskSource;
    private TaskSource outputTaskSource;

    private final List<ProcessingUnit> units = new ArrayList<ProcessingUnit>();

    public interface LocalPluginTask
            extends Task
    {
        // TODO
        @Config("in:type")
        public JsonNode getInputType();

        // TODO
        @Config("out:type")
        public JsonNode getOutputType();
    }

    @Inject
    public LocalExecutor(ModelManager modelManager, PluginManager pluginManager)
    {
        this.modelManager = modelManager;
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
        LocalPluginTask task = config.load(LocalPluginTask.class);
        in = newInputPlugin(task.getInputType());
        out = newOutputPlugin(task.getOutputType());
        inputTran = in.newInputTransaction(config);
        outputTran = out.newOutputTransaction(config);
        InputTask inputTask = inputTran.getInputTask();
        OutputTask outputTask = outputTran.getOutputTask(inputTask);

        processorCount = inputTask.getProcessorCount();
        inputTaskSource = config.dumpTask(inputTask);
        outputTaskSource = config.dumpTask(outputTask);

        validateTransaction();
    }

    private void validateTransaction()
    {
        // TODO
        System.out.println("input: "+inputTaskSource);
        System.out.println("output: "+outputTaskSource);
    }

    public void begin()
    {
        inputTran.begin();
        outputTran.begin();
    }

    private static class ProcessingUnit
    {
        private final InputProcessor inputProc;
        private final OutputOperator outputOp;
        private final MonitoringOperator monitorOp;
        private Report inputReport;
        private Report outputReport;

        public ProcessingUnit(InputProcessor inputProc, OutputOperator outputOp,
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
        for (int procIndex=0; procIndex < processorCount; procIndex++) {
            OutputOperator outputOp = out.openOutputOperator(outputTaskSource, procIndex);
            try {
                MonitoringOperator monitorOp = new MonitoringOperator(outputOp);
                InputProcessor inputProc = in.startInputProcessor(inputTaskSource, procIndex, monitorOp);
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
