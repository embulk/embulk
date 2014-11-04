package org.quickload.exec;

import java.util.List;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.config.FailedReport;
import org.quickload.record.Schema;
import org.quickload.spi.ProcControl;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcTaskSource;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.PluginThread;
import org.quickload.channel.PageChannel;

public class LocalExecutor
{
    private final Injector injector;

    private ConfigSource config;
    private InputPlugin in;
    private OutputPlugin out;
    private TaskSource taskSource;

    //private final List<ProcessingUnit> units = new ArrayList<ProcessingUnit>();

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

        public ProcTaskSource getProcTask();
        public void setProcTask(ProcTaskSource procTaskSource);

        public TaskSource getInputTask();
        public void setInputTask(TaskSource taskSource);

        public TaskSource getOutputTask();
        public void setOutputTask(TaskSource taskSource);
    }

    @Inject
    public LocalExecutor(Injector injector)
    {
        this.injector = injector;
    }

    public void configure(ConfigSource config)
    {
        this.config = config;
        ExecutorTask task = config.loadTask(ExecutorTask.class);

        ProcTask proc = new ProcTask(injector);

        in = proc.newPlugin(InputPlugin.class, task.getInputType());
        out = proc.newPlugin(OutputPlugin.class, task.getOutputType());

        task.setInputTask(in.getInputTask(proc, config));
        task.setOutputTask(out.getOutputTask(proc, config));
        task.setProcTask(proc.dump());

        this.taskSource = config.dumpTask(task);

        System.out.println("input: "+task.getInputTask());
        System.out.println("output: "+task.getOutputTask());
    }

    private static class ProcessContext
    {
        private final Report[] inputReports;
        private final Report[] outputReports;

        public ProcessContext(int processorCount)
        {
            this.inputReports = new Report[processorCount];
            this.outputReports = new Report[processorCount];
            for (int i=0; i < processorCount; i++) {
                inputReports[i] = outputReports[i] = new Report();
            }
        }

        public void setInputReport(int processorIndex, Report report)
        {
            inputReports[processorIndex] = report;
        }

        public void setOutputReport(int processorIndex, Report report)
        {
            outputReports[processorIndex] = report;
        }

        public List<Report> getInputReports()
        {
            return ImmutableList.copyOf(inputReports);
        }

        public List<Report> getOutputReports()
        {
            return ImmutableList.copyOf(outputReports);
        }
    }

    public void run()
    {
        final ExecutorTask task = taskSource.loadTask(ExecutorTask.class);
        final ProcTask proc = ProcTask.load(injector, task.getProcTask());
        final ProcessContext context = new ProcessContext(proc.getProcessorCount());

        in.runInputTransaction(proc, task.getInputTask(), new ProcControl() {
            public List<Report> run()
            {
                out.runOutputTransaction(proc, task.getOutputTask(), new ProcControl() {
                    public List<Report> run()
                    {
                        process(proc, task, context);
                        return context.getOutputReports();
                    }
                });
                return context.getInputReports();
            }
        });
    }

    private static void process(final ProcTask proc, final ExecutorTask task, final ProcessContext context)
    {
        final InputPlugin in = proc.newPlugin(InputPlugin.class, task.getInputType());
        final OutputPlugin out = proc.newPlugin(OutputPlugin.class, task.getOutputType());

        // TODO multi-threading

        for (int i=0; i < proc.getProcessorCount(); i++) {
            final int processorIndex = i;

            try (final PageChannel channel = proc.newPageChannel()) {
                proc.startPluginThread(new PluginThread() {
                    public void run()
                    {
                        try {
                            // TODO return Report
                            Report report = out.runOutput(proc, task.getOutputTask(),
                                processorIndex, channel.getInput());
                            context.setOutputReport(processorIndex, report);
                        } catch (Exception ex) {
                            context.setOutputReport(processorIndex, new FailedReport(ex));
                        } finally {
                            channel.completeConsumer();
                        }
                    }
                });

                try {
                    Report report = in.runInput(proc, task.getInputTask(),
                            processorIndex, channel.getOutput());
                    channel.completeProducer();
                    channel.join();
                    context.setInputReport(processorIndex, report);
                } catch (Exception ex) {
                    context.setInputReport(processorIndex, new FailedReport(ex));
                }
            }
        }
    }
}
