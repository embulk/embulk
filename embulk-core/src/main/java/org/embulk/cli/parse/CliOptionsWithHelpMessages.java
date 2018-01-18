package org.embulk.cli.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * CliOptionsWithHelpMessages is an extension of commons-cli's org.apache.commons.cli.Options.
 *
 * It just recognizes {@code HelpMessageAsCliOption} while others are same with {@code org.apache.commons.cli.Options}.
 * {@code HelpMessageAsCliOption} is added just as a help message line, but ignored as a command line option.
 *
 * It is visible only in |org.embulk.cli.parse| because it is an extension of a commons-cli class.
 * Dependencies on third-party libraries are to be encapsulated.
 */
final class CliOptionsWithHelpMessages extends Options {
    CliOptionsWithHelpMessages() {
        this.allOptions = new ArrayList<Option>();
    }

    /** Adds commons-cli's {@code org.apache.commons.cli.Option}. */
    @Override
    public final Options addOption(final Option option) {
        this.allOptions.add(option);
        if (option instanceof HelpMessageAsCliOption) {
            return this;
        }
        return super.addOption(option);
    }

    final List<Option> getAllOptions() {
        return Collections.unmodifiableList(this.allOptions);
    }

    private final ArrayList<Option> allOptions;
}
