package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.Report;

public interface ExecControl
{
    public List<Report> run(TaskSource taskSource);
}
