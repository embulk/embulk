---
EEP: unnumbered
Title: Expose Task report
Author: thanhle
Status: draft
Type: Standards Track
Created: 2023-04-14
---

Introduction
=============

Currently, `TaskReport` information is only used internally by `InputPlugin` and `OutputPlugin`. This EEP propose a change to expose `TaskReport` to outside also add a new SPI, change in `Formatter` and `Parser` help them able to provide a TaskReport information.

Motivation
===========

The TaskReport now mainly is used by `InputPlugin` and `OutputPlugin` for creating a `ConfigDiff`. Moreover, the Formatter and Parser Plugin can't provide TaskReport.

This behavior limit plugin and application able to add and get these reports relate to task, e.g. The InputPlugin can provide statistical information of task such as num of records, num of error, file size etc.

Plugin SPI Definitions
=======================

This section explains the additional Embulk plugin SPI definition

Definitions
------------

### Changing in interface `ParserPlugin`

```java
package org.embulk.spi;
public interface ParserPlugin {
    // ... (snip) ...
    /**
     * Runs each parsing task and return TaskReport
     *
     * @param taskSource  a configuration processed for the task from {@link ConfigSource}
     * @param schema  {@link Schema} to be parsed to
     * @param input  {@link FileOutput} that is read from a File Input Plugin, or a Decoder Plugin
     * @param output  {@link PageOutput} to write parsed input so that the input is read from an Output Plugin, or
     *     another Filter Plugin
     * @return the {@link TaskReport} in {@link Optional}
     */
    default Optional<TaskReport> runWithTaskReport(TaskSource taskSource, Schema schema, FileInput input, PageOutput output) {
        this.run(taskSource, schema, input, output);
        return Optional.empty();
    }
}
```

Changing in Embulk
=======================

This section explains the change in Embulk

Definitions
------------

### Changing in class `ExecutionResult`

`ExecutionResult` class will add two properties `inputTaskReports` and `outputTaskReports` and corresponding getter method.

```java
package org.embulk.exec;
public class ExecutionResult {
    // ... (snip) ...
    private List<TaskReport> inputTaskReports = new ArrayList<>();
    private List<TaskReport> outputTaskReports = new ArrayList<>();
    public ExecutionResult(ConfigDiff configDiff, boolean skipped, List<Throwable> ignoredExceptions,
                           List<TaskReport> inputTaskReports, List<TaskReport> outputTaskReports) {
        this(configDiff, skipped, ignoredExceptions);
        this.inputTaskReports = inputTaskReports;
        this.outputTaskReports = outputTaskReports;
    }
    public List<TaskReport> getOutputTaskReports() {
        return outputTaskReports;
    }
    public List<TaskReport> getInputTaskReports() {
        return inputTaskReports;
    }
}
```

### Changing in class `FileInputRunner`

```java
package org.embulk.exec;
public interface FileInputRunner {
    // ... (snip) ...
    @Override
    public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex,
                          PageOutput output) {
        final RunnerTask task = loadRunnerTaskFromTaskSource(taskSource);
        List<DecoderPlugin> decoderPlugins = newDecoderPlugins(task);
        ParserPlugin parserPlugin = newParserPlugin(task);
        final TransactionalFileInput tran = fileInputPlugin.open(task.getFileInputTaskSource(), taskIndex);
        try (CloseResource closer = new CloseResource(tran)) {
            try (AbortTransactionResource aborter = new AbortTransactionResource(tran)) {
                FileInput fileInput = DecodersInternal.open(decoderPlugins, task.getDecoderTaskSources(), tran);
                closer.closeThis(fileInput);
                final Optional<TaskReport> optionalParserTaskReport
                        = parserPlugin.runWithTaskReport(task.getParserTaskSource(), schema, fileInput, output);
                TaskReport report = tran.commit();  // TODO check output.finish() is called. wrap
                aborter.dontAbort();
                optionalParserTaskReport.ifPresent(report -> {
                    r.setNested("parser", r);
                });
                return report;
            }
        }
    }
}
```

### Changing in class `FileOutputRunner`

`FileOutputRunner` will check if output is instanceof `TransactionalPageOutput`, it will call a commit method to get `TaskReport`. This change implies if `Formatter` Plugin want to provide a `TaskReport` it should create a class which implement `TransactionalPageOutput` interface.

```java
package org.embulk.exec;
public interface FileOutputRunner {
    // ... (snip) ...
    @Override
    public TaskReport commit() {
        // TODO check finished
        TaskReport taskReport = tran.commit();
        if (output instanceof TransactionalPageOutput) {
            TaskReport outputTaskReport = ((TransactionalPageOutput) output).commit();
            if (taskReport != null && outputTaskReport != null) {
                taskReport.setNested("formatter", outputTaskReport);
            } else if (taskReport == null) {
                taskReport = outputTaskReport;
            }
        }
        return taskReport;
    }
}
```

### Changing in class `Report`
```java
package org.embulk.exec;
import java.util.List;
import org.embulk.config.TaskReport;
public class Report {
    private final List<TaskReport> input;
    private final List<TaskReport> output;
    public Report(List<TaskReport> input, List<TaskReport> output) {
        this.input = input;
        this.output = output;
    }
    public List<TaskReport> getInput() {
        return input;
    }
    public List<TaskReport> getOutput() {
        return output;
    }
}
```

### Changing in class `CommandLineImpl`

This change add a new option to write task report.

```java
package org.embulk.deps.cli;
public class CommandLineImpl {
    // ... (snip) ...
    static final Option TASK_REPORT = Option.builder("t").longOpt("task-report").hasArg().argName("PATH")
            .desc("Path to a file of task report").build();
}
```

Discussion for Decoder, Encoder and Filter Plugin
========================
In this EEP we don't propose a change to get TaskReport from Decoder, Encoder and Filter plugins because of some reasons

* `Decoder/Encoder`: It will create a FileInput/FileOutput and wrapper to TransactionalFileInput/TransactionalFileOutput return by FileInput and FileOutput. If a user wants to create a report, he needs to create TransactionalFileInput/TransactionalFileOutput in Decoder/Encoder. But, It has a high risk because every decoder/encoder in decoder/encoder chain must be created a TransactionalFileInput/Output and merge TaskReport of a downstream object. If They don’t follow this rule, We can't get TaskReport of the FileInput/Output plugin. Moreover, We don’t have a useful use case that the Decoder/Encoder needs to provide a report.
* `Filter`: Besides the same reason as `Decode/Encoder`, The TaskState currently only stores TaskReport of InputPlugin and OutputPlugin. If we want to add TaskReport for Filter It will require us to change a TaskState structure and add more change in Embulk core.

Backwards Compatibility
========================

* The new Embulk core will keep compatibility with older plugins by providing a default implementation for new method in `ParserPlugin`
* A newer plugin should provide implement for `run` method for backward compatible with old Embulk core.

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.