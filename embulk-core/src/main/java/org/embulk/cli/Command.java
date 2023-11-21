package org.embulk.cli;

public enum Command {
    BUNDLE("bundle"),
    CLEANUP("cleanup"),
    EXAMPLE("example"),
    EXEC("exec"),
    GEM("gem"),
    GUESS("guess"),
    INSTALL("install"),
    IRB("irb"),
    LICENSE("license"),
    MIGRATE("migrate"),
    MKBUNDLE("mkbundle"),
    NEW("new"),
    PREVIEW("preview"),
    RUN("run"),
    SELFUPDATE("selfupdate"),
    UNKNOWN("N/A"),
    NONE("(null)"),
    ;

    private Command(final String command) {
        this.command = command;
    }

    public static Command of(final String command) {
        if (command == null) {
            return NONE;
        }

        for (final Command constant : values()) {
            if (constant == UNKNOWN || constant == NONE) {
                continue;
            }
            if (constant.toString().equals(command)) {
                return constant;
            }
        }
        return UNKNOWN;
    }

    @Override
    public final String toString() {
        return this.command;
    }

    private final String command;
}
