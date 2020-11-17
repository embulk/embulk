package org.embulk.deps.cli;

import org.apache.commons.cli.Option;

final class PlaceholderOption extends Option {
    public PlaceholderOption(final String message) {
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
        return new PlaceholderOption(this.getDescription());
    }
}
