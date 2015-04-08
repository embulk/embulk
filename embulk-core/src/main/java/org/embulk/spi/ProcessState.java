package org.embulk.spi;

public interface ProcessState
{
    public void initialize(int inputTaskCount, int outputTaskCount);

    public TaskState getInputTaskState(int inputTaskIndex);

    public TaskState getOutputTaskState(int outputTaskIndex);
}
