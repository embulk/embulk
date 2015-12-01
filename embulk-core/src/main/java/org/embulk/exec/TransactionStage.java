package org.embulk.exec;

public enum TransactionStage
{
    INPUT_BEGIN(1),
    FILTER_BEGIN(2),
    EXECUTOR_BEGIN(3),
    OUTPUT_BEGIN(4),
    RUN(5),
    OUTPUT_COMMIT(6),
    EXECUTOR_COMMIT(7),
    FILTER_COMMIT(8),
    INPUT_COMMIT(9),
    CLEANUP(10);

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
