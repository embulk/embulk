package org.embulk.cli;

import java.util.List;
import java.util.Properties;

public abstract class CommandLine {
    public abstract boolean isValid();

    public abstract Command getCommand();

    public abstract List<String> getArguments();

    public abstract Properties getCommandLineProperties();

    public abstract String getStdOut();

    public abstract String getStdErr();
}
