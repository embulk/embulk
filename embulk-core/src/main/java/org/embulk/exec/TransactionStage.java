package org.embulk.exec;

public enum TransactionStage
{
    ERROR_DATA_BEGIN(1),
    INPUT_BEGIN(2),
    FILTER_BEGIN(3),
    EXECUTOR_BEGIN(4),
    OUTPUT_BEGIN(5),
    RUN(6),
    OUTPUT_COMMIT(7),
    EXECUTOR_COMMIT(8),
    FILTER_COMMIT(9),
    INPUT_COMMIT(10),
    ERROR_DATA_COMMIT(11),
    CLEANUP(12);

    private final int index;

    private TransactionStage(int index)
    {
        this.index = index;
    }

    public boolean isBefore(TransactionStage another)
    {
        return index < another.index;
    }
}
