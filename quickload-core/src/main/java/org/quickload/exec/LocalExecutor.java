package org.quickload.exec;

import java.util.List;
import java.util.ArrayList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.base.Function;

import org.quickload.config.ConfigSource;
import org.quickload.in.LocalFileCsvInput;
import org.quickload.out.LocalFileCsvOutput;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.InputTransaction;
import org.quickload.spi.OutputTransaction;
import org.quickload.spi.InputTask;
import org.quickload.spi.OutputTask;
import org.quickload.spi.InputProcessor;
import org.quickload.spi.OutputOperator;
import org.quickload.spi.Report;

public class LocalExecutor
        implements AutoCloseable
{
    private ConfigSource config;
    private InputPlugin in;
    private OutputPlugin out;

    private InputTransaction inputTran;
    private OutputTransaction outputTran;

    private InputTask inputTask;
    private OutputTask outputTask;

    private List<ProcessingUnit> units = new ArrayList<ProcessingUnit>();

    protected InputPlugin newInputPlugin()
    {
        return new LocalFileCsvInput();  // TODO
    }

    protected OutputPlugin newOutputPlugin()
    {
        return new LocalFileCsvOutput();  // TODO
    }

    public void configure(ConfigSource config)
    {
        this.config = config;
        in = newInputPlugin();
        out = newOutputPlugin();
        inputTran = in.newInputTransaction(config);
        outputTran = out.newOutputTransaction(config);
        inputTask = inputTran.getInputTask();
        outputTask = outputTran.getOutputTask(inputTask);
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
            OutputOperator outputOp = out.openOperator(outputTask, procIndex);
            try {
                MonitoringOperator monitorOp = new MonitoringOperator(outputOp);
                InputProcessor inputProc = in.startProcessor(inputTask, procIndex, monitorOp);
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
