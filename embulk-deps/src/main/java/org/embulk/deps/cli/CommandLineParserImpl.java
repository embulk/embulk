package org.embulk.deps.cli;

import java.util.List;
import org.slf4j.Logger;

// It is public just to be accessed from embulk-core.
public final class CommandLineParserImpl extends org.embulk.cli.CommandLineParser {
    public CommandLineParserImpl() {}

    @Override
    public CommandLineImpl parse(final List<String> originalArgs, final Logger logger) {
        return CommandLineImpl.of(originalArgs, logger);
    }
}
