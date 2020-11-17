package org.embulk.deps.cli;

import java.io.PrintWriter;
import java.io.StringWriter;

final class CommandWriters {
    CommandWriters() {
        this.stdoutBuffer = new StringWriter();
        this.stdoutWriter = new PrintWriter(this.stdoutBuffer);
        this.stderrBuffer = new StringWriter();
        this.stderrWriter = new PrintWriter(this.stderrBuffer);
    }

    final void printlnOut(final String string) {
        this.stdoutWriter.println(string);
    }

    final void printlnErr(final String string) {
        this.stderrWriter.println(string);
    }

    final PrintWriter getStdOutWriter() {
        return this.stdoutWriter;
    }

    final PrintWriter getStdErrWriter() {
        return this.stderrWriter;
    }

    final String getStdOut() {
        return this.stdoutBuffer.toString();
    }

    final String getStdErr() {
        return this.stderrBuffer.toString();
    }

    private final StringWriter stdoutBuffer;
    private final PrintWriter stdoutWriter;
    private final StringWriter stderrBuffer;
    private final PrintWriter stderrWriter;
}
