package org.embulk.cli.parse;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * CliHelpFormatterWithHelpMessages is an extension of commons-cli's org.apache.commons.cli.HelpFormatter.
 *
 * It recognizes {@code HelpMessageAsCliOption} from {@code CliOptionsWithHelpMessages} in addition to that
 * {@code org.apache.commons.cli.HelpFormatter} processes ordinary {@code org.apache.commons.cli.Option}.
 *
 * It combines both {@code HelpMessageAsCliOption} and ordinary {@code org.apache.commons.cli.Option}, and
 * builds the entire help message.
 *
 * It is visible only in {@code org.embulk.cli.parse} because it is an extension of a commons-cli class.
 * Dependencies on third-party libraries are to be encapsulated.
 */
final class CliHelpFormatterWithHelpMessages extends HelpFormatter {
    CliHelpFormatterWithHelpMessages(final String syntaxPrefix, final int prefixWidth) {
        super();
        this.prefixWidth = prefixWidth;
        this.setSyntaxPrefix(syntaxPrefix);
    }

    @Override
    public final void printHelp(
            final PrintWriter printWriter,
            final int width,
            final String commandLineSyntax,
            final String header,
            final Options options,
            final int leftPaddingWidth,
            final int descriptionPaddingWidth,
            final String footer,
            final boolean autoUsage) {
        super.printHelp(
                printWriter,
                width,
                commandLineSyntax,
                header,
                options,
                leftPaddingWidth,
                descriptionPaddingWidth,
                footer,
                autoUsage);
        printWriter.flush();
    }

    @Override
    protected final StringBuffer renderOptions(
            final StringBuffer buffer,
            final int width,
            final Options options,
            final int leftPaddingWidth,
            final int descriptionPaddingWidth) {
        // If |options| is not the extended version, it is just delegated to the original HelpFormatter.
        if (!(options instanceof CliOptionsWithHelpMessages)) {
            return super.renderOptions(buffer, width, options, leftPaddingWidth, descriptionPaddingWidth);
        }

        final List<Option> allOptions = ((CliOptionsWithHelpMessages) options).getAllOptions();
        final String leftPadding = super.createPadding(leftPaddingWidth);
        final String descriptionPadding = super.createPadding(descriptionPaddingWidth);

        int maxOptionLinePrefixLength = 0;
        final HashMap<Option, String> optionLinePrefixes = new HashMap<Option, String>();

        // The first pass: builds all "prefixes" of option lines, and measures their length.
        for (final Option option : allOptions) {
            final StringBuilder optionLinePrefixBuilder = new StringBuilder();

            if (!(option instanceof HelpMessageAsCliOption)) {
                optionLinePrefixBuilder.append(leftPadding);
                if (option.getOpt() == null) {
                    optionLinePrefixBuilder.append("    ").append(getLongOptPrefix()).append(option.getLongOpt());
                } else {
                    optionLinePrefixBuilder.append(getOptPrefix()).append(option.getOpt());
                    if (option.hasLongOpt()) {
                        optionLinePrefixBuilder.append(", ").append(getLongOptPrefix()).append(option.getLongOpt());
                    }
                }

                if (option.hasArg()) {
                    final String argName = option.getArgName();
                    if (argName != null && argName.length() == 0) {
                        optionLinePrefixBuilder.append(' ');
                    } else {
                        optionLinePrefixBuilder.append(option.hasLongOpt() ? getLongOptSeparator() : " ");
                        optionLinePrefixBuilder.append(argName != null ? option.getArgName() : getArgName());
                    }
                }

                optionLinePrefixes.put(option, optionLinePrefixBuilder.toString());
                if (maxOptionLinePrefixLength < optionLinePrefixBuilder.length()) {
                    maxOptionLinePrefixLength = optionLinePrefixBuilder.length();
                }
            }
        }

        if (maxOptionLinePrefixLength < this.prefixWidth) {
            maxOptionLinePrefixLength = this.prefixWidth;
        }

        // The second pass: builds all lines both from options and help messages.
        for (final Option option : allOptions) {
            if (option instanceof HelpMessageAsCliOption) {
                buffer.append(option.toString());
            } else {
                final StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append(optionLinePrefixes.get(option));
                if (lineBuilder.length() < maxOptionLinePrefixLength) {
                    lineBuilder.append(super.createPadding(maxOptionLinePrefixLength - lineBuilder.length()));
                }
                lineBuilder.append(descriptionPadding);

                if (option.getDescription() != null) {
                    lineBuilder.append(option.getDescription());
                }
                super.renderWrappedText(buffer,
                                        width,
                                        maxOptionLinePrefixLength + descriptionPaddingWidth,
                                        lineBuilder.toString());
            }
            buffer.append(super.getNewLine());
        }
        return buffer;
    }

    private final int prefixWidth;
}
