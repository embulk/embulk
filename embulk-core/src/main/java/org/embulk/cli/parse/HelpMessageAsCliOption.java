package org.embulk.cli.parse;

import org.apache.commons.cli.Option;

final class HelpMessageAsCliOption extends Option {
    public HelpMessageAsCliOption(final String message) {
        super("_", message);
    }

    @Override
    public final String toString() {
        return this.getDescription();
    }

    @Override
    public final boolean equals(Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return this.getDescription().hashCode();
    }

    @Override
    public final Object clone() {
        return new HelpMessageAsCliOption(this.getDescription());
    }
}
