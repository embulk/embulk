package org.embulk.test;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.PreviewResult;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

/**
 * This plugin is used for TestingEmbulk.InputBuilder.preview().
 */
public final class PreviewResultInputPlugin implements InputPlugin {
    private static PreviewResult previewResult;

    public static void setPreviewResult(PreviewResult result) {
        previewResult = result;
    }

    @Override
    public ConfigDiff transaction(ConfigSource config, Control control) {
        checkState(previewResult != null, "PreviewResult object must be set");
        return resume(config.loadConfig(Task.class).dump(), previewResult.getSchema(), 1, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount, Control control) {
        control.run(taskSource, schema, taskCount);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource, Schema schema, int taskCount, List<TaskReport> successTaskReports) {}

    @Override
    public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex, PageOutput output) {
        for (Page page : previewResult.getPages()) {
            output.add(page);
        }
        return Exec.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return Exec.newConfigDiff();
    }
}
