package org.embulk.exec;

import java.util.ArrayList;
import java.util.List;

public class StatisticsResult
{
    private final long totalRecords;
    private final long successRecords;
    private final long errorRecords;
    private final List<StatisticsFileResult> filesResult;

    public StatisticsResult(final long totalRecords, final long successRecords, final long errorRecords)
    {
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.errorRecords = errorRecords;
        this.filesResult = new ArrayList<>();
    }

    public StatisticsResult(final long totalRecords, final long successRecords, final long errorRecords, final List<StatisticsFileResult> filesResult)
    {
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.errorRecords = errorRecords;
        this.filesResult = filesResult;
    }

    public List<StatisticsFileResult> getFilesResult()
    {
        return filesResult;
    }

    public long getTotalRecords()
    {
        return totalRecords;
    }

    public long getSuccessRecords()
    {
        return successRecords;
    }

    public long getErrorRecords()
    {
        return errorRecords;
    }

    @Override
    public String toString()
    {
        return "StatisticsResult{" +
                "totalRecords=" + totalRecords +
                ", successRecords=" + successRecords +
                ", errorRecords=" + errorRecords +
                ", filesResult=" + filesResult +
                '}';
    }
}
