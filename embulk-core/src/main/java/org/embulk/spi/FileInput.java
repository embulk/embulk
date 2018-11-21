package org.embulk.spi;

import java.util.Optional;

public interface FileInput extends AutoCloseable {
    boolean nextFile();

    Buffer poll();

    void close();

    default void setFileName(int taskIndex, String fileName) {
        report.setFileName(taskIndex, fileName);
    }

    default void setExpectedSize(int taskIndex, long expectedSize) {
        report.setExpectedSize(taskIndex, expectedSize);
    }

    default Optional<String> fileName(int taskIndex) {
        return Optional.ofNullable(report.getFileName(taskIndex));
    }

    default Optional<Long> expectedSize(int taskIndex) {
        return Optional.of(report.getExpectedSize(taskIndex));
    }

    FileInputReport report = new FileInputReport();
}
