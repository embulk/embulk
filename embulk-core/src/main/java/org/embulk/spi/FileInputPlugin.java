package org.embulk.spi;

import java.util.List;
import java.util.Optional;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;

public interface FileInputPlugin {
    interface Control {
        List<TaskReport> run(TaskSource taskSource,
                int taskCount);
    }

    ConfigDiff transaction(ConfigSource config,
            FileInputPlugin.Control control);

    ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileInputPlugin.Control control);

    void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports);

    TransactionalFileInput open(TaskSource taskSource,
            int taskIndex);

    default void setFileName(int taskIndex, String fileName) {
        report.setFileName(taskIndex, fileName);
    }

    default void setExpectedSize(int taskIndex, long expectedSize) {
        report.setExpectedSize(taskIndex, expectedSize);
    }

    default Optional<String> fileName(int taskIndex) {
        return Optional.ofNullable(report.getFileName(taskIndex));
    }

    default Optional<Long> expectedSize(int taskIndex) {
        return Optional.of(report.getExpectedSize(taskIndex));
    }

    FileInputReport report = new FileInputReport();
}
