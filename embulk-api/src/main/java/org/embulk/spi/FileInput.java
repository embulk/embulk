package org.embulk.spi;

import java.util.Optional;

public interface FileInput extends AutoCloseable {
    boolean nextFile();

    Buffer poll();

    void close();

    // Gets a text that hints the name of the current file input.
    //
    // <p>The hint is aimed for logging, not for any data recorded in rows / columns.
    // There is no any guarantee on the text. The text format may change in future versions.
    // The text may be lost by putting another plugin in the configuration.
    //
    // @return the hint text
    default Optional<String> hintOfCurrentInputFileNameForLogging() {
        return Optional.empty();
    }
}
