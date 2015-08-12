package org.embulk.spi;

public interface ProcessState
{
    void initialize(int inputTaskCount, int outputTaskCount);

    TaskState getInputTaskState(int inputTaskIndex);

    TaskState getOutputTaskState(int outputTaskIndex);
}
