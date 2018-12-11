package org.embulk.spi;

import java.util.Optional;

public interface FileInput extends AutoCloseable {
    boolean nextFile();

    Buffer poll();

    void close();

    default Optional<String> hintOfCurrentInputFileNameForLogging() {
        return Optional.empty();
    }
}
