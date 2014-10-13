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

    private InputTask inputTask;
    private OutputTask outputTask;

    private final List<ProcessingUnit> units = new ArrayList<ProcessingUnit>();

    public interface LocalPluginTask
            extends Task
    {
        // TODO
        @Config("ConfigExpression")
        public JsonNode getConfigExpression();
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
        in = newInputPlugin(task.getConfigExpression());
        out = newOutputPlugin(task.getConfigExpression());
        inputTran = in.newInputTransaction(config);
        outputTran = out.newOutputTransaction(config);
        inputTask = inputTran.getInputTask();
        outputTask = outputTran.getOutputTask(inputTask);
        validateTransaction();
    }

    private void validateTransaction()
    {
        // InputTask and OutputTask must be serializable
        try {
            // TODO add ModelManager.serialize method
            String serialized = modelManager.writeJson(inputTask);
            System.out.println("serialized input task: "+serialized);  // XXX
        } catch (RuntimeException ex) {
            throw new AssertionError(String.format("InputTask '%s' must be serializable", inputTask.getClass()), ex);
        }
        try {
            String serialized = modelManager.writeJson(outputTask);
            System.out.println("serialized output task: "+serialized);  // XXX
        } catch (RuntimeException ex) {
            throw new AssertionError(String.format("OutputTask '%s' must be serializable", outputTask.getClass()), ex);
        }
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
        for (int procIndex=0; procIndex < inputTask.getProcessorCount(); procIndex++) {
            OutputOperator outputOp = out.openOutputOperator(outputTask, procIndex);
            try {
                MonitoringOperator monitorOp = new MonitoringOperator(outputOp);
                InputProcessor inputProc = in.startInputProcessor(inputTask, procIndex, monitorOp);
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
