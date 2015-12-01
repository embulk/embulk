package org.embulk.exec;

public class PartialExecutionException
        extends RuntimeException
{
    private final ResumeState resumeState;
    private final TransactionStage transactionStage;

    public PartialExecutionException(Throwable cause, ResumeState resumeState,
            TransactionStage transactionStage)
    {
        super(cause);
        this.resumeState = resumeState;
        this.transactionStage = transactionStage;
    }

    public ResumeState getResumeState()
    {
        return resumeState;
    }

    public TransactionStage getTransactionStage()
    {
        return transactionStage;
    }
}
