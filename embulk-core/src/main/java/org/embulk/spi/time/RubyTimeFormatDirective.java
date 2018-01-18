package org.embulk.spi.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RubyTimeFormatDirective enumerates constants to represent Ruby-compatible time format directives.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 *
 * @see <a href="https://docs.ruby-lang.org/en/2.4.0/Time.html#method-i-strftime">Ruby v2.4.0's datetime format</a>
 */
enum RubyTimeFormatDirective {
    // Date (Year, Month, Day):

    YEAR_WITH_CENTURY(true, 'Y'),
    CENTURY(true, 'C'),
    YEAR_WITHOUT_CENTURY(true, 'y'),

    MONTH_OF_YEAR(true, 'm'),
    MONTH_OF_YEAR_FULL_NAME(false, 'B'),
    MONTH_OF_YEAR_ABBREVIATED_NAME(false, 'b'),
    MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H(false, 'h'),

    DAY_OF_MONTH_ZERO_PADDED(true, 'd'),
    DAY_OF_MONTH_BLANK_PADDED(true, 'e'),

    DAY_OF_YEAR(true, 'j'),

    // Time (Hour, Minute, Second, Subsecond):

    HOUR_OF_DAY_ZERO_PADDED(true, 'H'),
    HOUR_OF_DAY_BLANK_PADDED(true, 'k'),
    HOUR_OF_AMPM_ZERO_PADDED(true, 'I'),
    HOUR_OF_AMPM_BLANK_PADDED(true, 'l'),
    AMPM_OF_DAY_LOWER_CASE(false, 'P'),
    AMPM_OF_DAY_UPPER_CASE(false, 'p'),

    MINUTE_OF_HOUR(true, 'M'),

    SECOND_OF_MINUTE(true, 'S'),

    MILLI_OF_SECOND(true, 'L'),
    NANO_OF_SECOND(true, 'N'),

    // Time zone:

    TIME_OFFSET(false, 'z'),
    TIME_ZONE_NAME(false, 'Z'),

    // Weekday:

    DAY_OF_WEEK_FULL_NAME(false, 'A'),
    DAY_OF_WEEK_ABBREVIATED_NAME(false, 'a'),
    DAY_OF_WEEK_STARTING_WITH_MONDAY_1(true, 'u'),
    DAY_OF_WEEK_STARTING_WITH_SUNDAY_0(true, 'w'),

    // ISO 8601 week-based year and week number:
    // The first week of YYYY starts with a Monday and includes YYYY-01-04.
    // The days in the year before the first week are in the last week of
    // the previous year.

    WEEK_BASED_YEAR_WITH_CENTURY(true, 'G'),
    WEEK_BASED_YEAR_WITHOUT_CENTURY(true, 'g'),
    WEEK_OF_WEEK_BASED_YEAR(true, 'V'),

    // Week number:
    // The first week of YYYY that starts with a Sunday or Monday (according to %U
    // or %W). The days in the year before the first week are in week 0.

    WEEK_OF_YEAR_STARTING_WITH_SUNDAY(true, 'U'),
    WEEK_OF_YEAR_STARTING_WITH_MONDAY(true, 'W'),

    // Seconds since the Epoch:

    SECOND_SINCE_EPOCH(true, 's'),
    MILLISECOND_SINCE_EPOCH(false, 'Q'),  // TODO: Revisit this "%Q" is not a numeric pattern?

    // Recurred:

    RECURRED_UPPER_C('c', "a b e H:M:S Y"),
    RECURRED_UPPER_D('D', "m/d/y"),
    RECURRED_LOWER_X('x', "m/d/y"),
    RECURRED_UPPER_F('F', "Y-m-d"),
    RECURRED_LOWER_N('n', "\n"),
    RECURRED_UPPER_R('R', "H:M"),
    RECURRED_LOWER_R('r', "I:M:S p"),
    RECURRED_UPPER_T('T', "H:M:S"),
    RECURRED_UPPER_X('X', "H:M:S"),
    RECURRED_LOWER_T('t', "\t"),
    RECURRED_LOWER_V('v', "e-b-Y"),
    RECURRED_PLUS('+', "a b e H:M:S Z Y"),
    ;

    private RubyTimeFormatDirective(final boolean isNumeric, final char conversionSpecifier) {
        this.conversionSpecifier = conversionSpecifier;
        this.isNumeric = isNumeric;
        this.isRecurred = false;
        this.recurred = null;
    }

    private RubyTimeFormatDirective(final char conversionSpecifier, final String recurred) {
        this.conversionSpecifier = conversionSpecifier;
        this.isNumeric = false;
        this.isRecurred = true;
        this.recurred = recurred;
    }

    private RubyTimeFormatDirective() {
        this(false, '\0');
    }

    static boolean isSpecifier(final char conversionSpecifier) {
        return FROM_CONVERSION_SPECIFIER.containsKey(conversionSpecifier);
    }

    static RubyTimeFormatDirective of(final char conversionSpecifier) {
        return FROM_CONVERSION_SPECIFIER.get(conversionSpecifier);
    }

    @Override
    public String toString() {
        return "" + this.conversionSpecifier;
    }

    boolean isNumeric() {
        return this.isNumeric;
    }

    List<RubyTimeFormatToken> toTokens() {
        return TO_TOKENS.get(this);
    }

    static {
        final HashMap<Character, RubyTimeFormatDirective> charDirectiveMapBuilt = new HashMap<>();
        for (final RubyTimeFormatDirective directive : values()) {
            if (directive.conversionSpecifier != '\0') {
                charDirectiveMapBuilt.put(directive.conversionSpecifier, directive);
            }
        }
        FROM_CONVERSION_SPECIFIER = Collections.unmodifiableMap(charDirectiveMapBuilt);

        final EnumMap<RubyTimeFormatDirective, List<RubyTimeFormatToken>> directiveTokensMapBuilt =
                new EnumMap<>(RubyTimeFormatDirective.class);
        for (final RubyTimeFormatDirective directive : values()) {
            // Non-recurred directives first so that recurred directives can use tokens of non-recurred directives.
            if (!directive.isRecurred) {
                final ArrayList<RubyTimeFormatToken> tokensBuilt = new ArrayList<>();
                tokensBuilt.add(new RubyTimeFormatToken.Directive(directive));
                directiveTokensMapBuilt.put(directive, Collections.unmodifiableList(tokensBuilt));
            }
        }
        for (final RubyTimeFormatDirective directive : values()) {
            if (directive.isRecurred) {
                final ArrayList<RubyTimeFormatToken> tokensBuilt = new ArrayList<>();
                for (int i = 0; i < directive.recurred.length(); ++i) {
                    final RubyTimeFormatDirective eachDirective =
                            charDirectiveMapBuilt.get(directive.recurred.charAt(i));
                    if (eachDirective == null) {
                        tokensBuilt.add(new RubyTimeFormatToken.Immediate(directive.recurred.charAt(i)));
                    } else {
                        tokensBuilt.add(directiveTokensMapBuilt.get(eachDirective).get(0));
                    }
                }
                directiveTokensMapBuilt.put(directive, Collections.unmodifiableList(tokensBuilt));
            }
        }
        TO_TOKENS = Collections.unmodifiableMap(directiveTokensMapBuilt);
    }

    private static final Map<Character, RubyTimeFormatDirective> FROM_CONVERSION_SPECIFIER;
    private static final Map<RubyTimeFormatDirective, List<RubyTimeFormatToken>> TO_TOKENS;

    private final char conversionSpecifier;
    private final boolean isNumeric;
    private final boolean isRecurred;
    private final String recurred;
}
