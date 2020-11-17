package org.embulk.deps.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * OptionsWithPlaceholders is an extension of {@code commons-cli}'s {@code org.apache.commons.cli.Options}.
 *
 * It just accepts {@link PlaceholderOption} while others are same with {@code org.apache.commons.cli.Options}.
 * {@code PlaceholderOption} is added just as a message line in help. It is ignored from parsing command line.
 */
final class OptionsWithPlaceholders extends Options {
    OptionsWithPlaceholders() {
        this.optionsWithPlaceholders = new ArrayList<Option>();
    }

    /** Adds commons-cli's {@code org.apache.commons.cli.Option}. */
    @Override
    public final OptionsWithPlaceholders addOption(final Option option) {
        this.optionsWithPlaceholders.add(option);
        if (option instanceof PlaceholderOption) {
            return this;
        }
        super.addOption(option);
        return this;
    }

    final List<Option> getOptionsToRender() {
        return Collections.unmodifiableList(this.optionsWithPlaceholders);
    }

    @Override
    public final OptionsWithPlaceholders clone() {
        final OptionsWithPlaceholders cloned = new OptionsWithPlaceholders();
        for (final Option option : this.optionsWithPlaceholders) {
            try {
                cloned.addOption((Option) option.clone());
            } catch (final RuntimeException ex) {
                throw new InternalError("Failed to clone org.apache.commons.cli.Option unexpectedly.", ex);
            }
        }
        return cloned;
    }

    private final ArrayList<Option> optionsWithPlaceholders;
}
