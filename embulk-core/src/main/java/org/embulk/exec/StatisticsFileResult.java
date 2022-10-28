package org.embulk.exec;

public class StatisticsFileResult
{
    private final String fileName;
    private final long totalRecords;
    private final long successRecords;
    private final long errorRecords;

    public StatisticsFileResult(final String fileName, final long totalRecords, final long successRecords, final long errorRecords)
    {
        this.fileName = fileName;
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.errorRecords = errorRecords;
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

    public String getFileName()
    {
        return fileName;
    }

    @Override
    public String toString()
    {
        return "StatisticsFileResult{" +
                "fileName='" + fileName + '\'' +
                ", totalRecords=" + totalRecords +
                ", successRecords=" + successRecords +
                ", errorRecords=" + errorRecords +
                '}';
    }

    public static final class StatisticsFileResultBuilder
    {
        private long totalRecords;
        private long successRecords;
        private long errorRecords;
        private String fileName;

        private StatisticsFileResultBuilder()
        {
        }

        public static StatisticsFileResultBuilder aStatisticsFileResult()
        {
            return new StatisticsFileResultBuilder();
        }

        public StatisticsFileResultBuilder withTotalRecords(long totalRecords)
        {
            this.totalRecords = totalRecords;
            return this;
        }

        public StatisticsFileResultBuilder withSuccessRecords(long successRecords)
        {
            this.successRecords = successRecords;
            return this;
        }

        public StatisticsFileResultBuilder withErrorRecords(long errorRecords)
        {
            this.errorRecords = errorRecords;
            return this;
        }

        public StatisticsFileResultBuilder withFileName(String fileName)
        {
            this.fileName = fileName;
            return this;
        }

        public StatisticsFileResult build()
        {
            return new StatisticsFileResult(fileName, totalRecords, successRecords, errorRecords);
        }
    }
}
