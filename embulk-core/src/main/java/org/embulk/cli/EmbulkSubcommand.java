package org.embulk.cli;

public enum EmbulkSubcommand {
    BUNDLE("bundle"),
    CLEANUP("cleanup"),
    EXAMPLE("example"),
    EXEC("exec"),
    GEM("gem"),
    GUESS("guess"),
    IRB("irb"),
    MIGRATE("migrate"),
    MKBUNDLE("mkbundle"),
    NEW("new"),
    PREVIEW("preview"),
    RUN("run"),
    SELFUPDATE("selfupdate"),
    VERSION_OUT("--version"),
    VERSION_ERR("-version"),
    ;

    private EmbulkSubcommand(final String subcommand) {
        this.subcommand = subcommand;
    }

    public static EmbulkSubcommand of(final String subcommand) throws EmbulkCommandLineException {
        if (!(subcommand.equals("--version")) && !(subcommand.equals("-version"))) {
            for (final EmbulkSubcommand constant : values()) {
                if (constant.toString().equals(subcommand)) {
                    return constant;
                }
            }
        }
        throw new EmbulkCommandLineException("Unknown subcommand " + subcommand + ".");
    }

    @Override
    public final String toString() {
        return this.subcommand;
    }

    private final String subcommand;
}
