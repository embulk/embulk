package org.embulk.spi;

import java.util.Optional;

public interface FileInput extends AutoCloseable {
    boolean nextFile();

    Buffer poll();

    void close();

    default Optional<String> name() {
        return Optional.empty();
    }

    default Optional<Long> expectedSize() {
        return Optional.empty();
    }
}
