package org.quickload.spi;

import java.util.List;
import org.quickload.config.Report;

public interface ProcControl
{
    public List<Report> run();
}
