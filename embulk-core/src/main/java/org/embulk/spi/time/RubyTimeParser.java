package org.embulk.spi.time;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RubyTimeParser is a Ruby-compatible time parser.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 *
 * This class is almost reimplementation of Ruby v2.3.1's ext/date/date_strptime.c. See its COPYING for license.
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup">ext/date/date_strptime.c</a>
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup">COPYING</a>
 */
class RubyTimeParser
{
    // day_names
    private static final String[] DAY_NAMES = new String[] {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    // month_names
    private static final String[] MONTH_NAMES = new String[] {
            "January", "February", "March", "April", "May", "June", "July", "August", "September",
            "October", "November", "December", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    // merid_names
    private static final String[] MERID_NAMES = new String[] {
            "am", "pm", "a.m.", "p.m."
    };

    public RubyTimeParser(final RubyTimeFormat format)
    {
        this.format = format;
    }

    public TimeParsed parse(final String text)
    {
        return new StringParser(text).parse(this.format);
    }

    private static class StringParser
    {
        /**
         * The regular expression is reimplemented from Ruby v2.3.1's ext/date/date_strptime.c.
         *
         * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l571">pat_source</a>
         */
        private static final Pattern ZONE_PARSE_REGEX = Pattern.compile("\\A(" +
                        "(?:gmt|utc?)?[-+]\\d+(?:[,.:]\\d+(?::\\d+)?)?" +
                        "|(?-i:[[\\p{Alpha}].\\s]+)(?:standard|daylight)\\s+time\\b" +
                        "|(?-i:[[\\p{Alpha}]]+)(?:\\s+dst)?\\b" +
                        ")", Pattern.CASE_INSENSITIVE);

        private final String text;

        private int pos;
        private boolean fail;

        private StringParser(String text)
        {
            this.text = text;

            this.pos = 0;
            this.fail = false;
        }

        private TimeParsed parse(final RubyTimeFormat format)
        {
            final RubyTimeParsed.Builder builder = TimeParsed.rubyBuilder(this.text);

            for (final RubyTimeFormat.TokenWithNext tokenWithNext : format) {
                final RubyTimeFormatToken token = tokenWithNext.getToken();

                if (!token.isDirective()) {
                    final RubyTimeFormatToken.Immediate stringToken = (RubyTimeFormatToken.Immediate) token;
                    final String str = stringToken.getContent();
                    for (int i = 0; i < str.length(); i++) {
                        final char c = str.charAt(i);
                        if (isSpace(c)) {
                            while (!isEndOfText(text, pos) && isSpace(text.charAt(pos))) {
                                pos++;
                            }
                        }
                        else {
                            if (isEndOfText(text, pos) || c != text.charAt(pos)) {
                                fail = true;
                            }
                            pos++;
                        }
                    }
                }
                else {
                    switch (((RubyTimeFormatToken.Directive)token).getFormatDirective()) {
                    case DAY_OF_WEEK_FULL_NAME: // %A - The full weekday name (``Sunday'')
                    case DAY_OF_WEEK_ABBREVIATED_NAME: { // %a - The abbreviated name (``Sun'')
                        final int dayIndex = findIndexInPatterns(DAY_NAMES);
                        if (dayIndex >= 0) {
                            builder.setDayOfWeekStartingWithSunday0(dayIndex % 7);
                            pos += DAY_NAMES[dayIndex].length();
                        }
                        else {
                            fail = true;
                        }
                        break;
                    }
                    case MONTH_OF_YEAR_FULL_NAME: // %B - The full month name (``January'')
                    case MONTH_OF_YEAR_ABBREVIATED_NAME:  // %b, %h - The abbreviated month name (``Jan'')
                    case MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H: {
                        final int monIndex = findIndexInPatterns(MONTH_NAMES);
                        if (monIndex >= 0) {
                            builder.setMonthOfYear(monIndex % 12 + 1);
                            pos += MONTH_NAMES[monIndex].length();
                        }
                        else {
                            fail = true;
                        }
                        break;
                    }
                    case CENTURY: { // %C - year / 100 (round down.  20 in 2009)
                        final long cent;
                        if (isNumberPattern(tokenWithNext.getNextToken())) {
                            cent = readDigits(2);
                        }
                        else {
                            cent = readDigitsMax();
                        }
                        builder.setCentury((int)cent);
                        break;
                    }
                    case DAY_OF_MONTH_ZERO_PADDED: // %d, %Od - Day of the month, zero-padded (01..31)
                    case DAY_OF_MONTH_BLANK_PADDED: { // %e, %Oe - Day of the month, blank-padded ( 1..31)
                        final long day;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            day = readDigits(1);
                        }
                        else {
                            day = readDigits(2);
                        }

                        if (!validRange(day, 1, 31)) {
                            fail = true;
                        }
                        builder.setDayOfMonth((int)day);
                        break;
                    }
                    case WEEK_BASED_YEAR_WITH_CENTURY: { // %G - The week-based year
                        final long year;
                        if (isNumberPattern(tokenWithNext.getNextToken())) {
                            year = readDigits(4);
                        }
                        else {
                            year = readDigitsMax();
                        }
                        builder.setWeekBasedYear((int)year);
                        break;
                    }
                    case WEEK_BASED_YEAR_WITHOUT_CENTURY: { // %g - The last 2 digits of the week-based year (00..99)
                        final long v = readDigits(2);
                        if (!validRange(v, 0, 99)) {
                            fail = true;
                        }
                        builder.setWeekBasedYearWithoutCentury((int)v);
                        break;
                    }
                    case HOUR_OF_DAY_ZERO_PADDED: // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                    case HOUR_OF_DAY_BLANK_PADDED: { // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                        final long hour;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            hour = readDigits(1);
                        }
                        else {
                            hour = readDigits(2);
                        }

                        if (!validRange(hour, 0, 24)) {
                            fail = true;
                        }
                        builder.setHour((int)hour);
                        break;
                    }
                    case HOUR_OF_AMPM_ZERO_PADDED: // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                    case HOUR_OF_AMPM_BLANK_PADDED: { // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                        final long hour;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            hour = readDigits(1);
                        }
                        else {
                            hour = readDigits(2);
                        }

                        if (!validRange(hour, 1, 12)) {
                            fail = true;
                        }
                        builder.setHour((int)hour);
                        break;
                    }
                    case DAY_OF_YEAR: { // %j - Day of the year (001..366)
                        final long day = readDigits(3);
                        if (!validRange(day, 1, 365)) {
                            fail = true;
                        }
                        builder.setDayOfYear((int)day);
                        break;
                    }
                    case MILLI_OF_SECOND: // %L - Millisecond of the second (000..999)
                    case NANO_OF_SECOND: { // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                        boolean negative = false;
                        if (isSign(text, pos)) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        final long v;
                        final int initPos = pos;
                        if (isNumberPattern(tokenWithNext.getNextToken())) {
                            if (((RubyTimeFormatToken.Directive)token).getFormatDirective() ==
                                  RubyTimeFormatDirective.MILLI_OF_SECOND) {
                                v = readDigits(3);
                            }
                            else {
                                v = readDigits(9);
                            }
                        }
                        else {
                            v = readDigitsMax();
                        }

                        builder.setNanoOfSecond((!negative ? v : -v) * (int) Math.pow(10, 9 - (pos - initPos)));
                        break;
                    }
                    case MINUTE_OF_HOUR: { // %M, %OM - Minute of the hour (00..59)
                        final long min = readDigits(2);
                        if (!validRange(min, 0, 59)) {
                            fail = true;
                        }
                        builder.setMinuteOfHour((int)min);
                        break;
                    }
                    case MONTH_OF_YEAR: { // %m, %Om - Month of the year, zero-padded (01..12)
                        final long mon = readDigits(2);
                        if (!validRange(mon, 1, 12)) {
                            fail = true;
                        }
                        builder.setMonthOfYear((int)mon);
                        break;
                    }
                    case AMPM_OF_DAY_UPPER_CASE: // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                    case AMPM_OF_DAY_LOWER_CASE: { // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                        final int meridIndex = findIndexInPatterns(MERID_NAMES);
                        if (meridIndex >= 0) {
                            builder.setAmPmOfDay(meridIndex % 2 == 0 ? 0 : 12);
                            pos += MERID_NAMES[meridIndex].length();
                        }
                        else {
                            fail = true;
                        }
                        break;
                    }
                    case MILLISECOND_SINCE_EPOCH: { // %Q - Number of milliseconds since 1970-01-01 00:00:00 UTC.
                        boolean negative = false;
                        if (isMinus(text, pos)) {
                            negative = true;
                            pos++;
                        }

                        final long sec = (negative ? -readDigitsMax() : readDigitsMax());

                        builder.setSecondSinceEpoch(sec / 1000L, sec % 1000L * (long) Math.pow(10, 6));
                        break;
                    }
                    case SECOND_OF_MINUTE: { // %S - Second of the minute (00..59)
                        final long sec = readDigits(2);
                        if (!validRange(sec, 0, 60)) {
                            fail = true;
                        }
                        builder.setSecondOfMinute((int)sec);
                        break;
                    }
                    case SECOND_SINCE_EPOCH: { // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                        boolean negative = false;
                        if (isMinus(text, pos)) {
                            negative = true;
                            pos++;
                        }

                        final long sec = readDigitsMax();
                        builder.setSecondSinceEpoch(!negative ? sec : -sec, 0);
                        break;
                    }
                    case WEEK_OF_YEAR_STARTING_WITH_SUNDAY: // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                    case WEEK_OF_YEAR_STARTING_WITH_MONDAY: { // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                        final long week = readDigits(2);
                        if (!validRange(week, 0, 53)) {
                            fail = true;
                        }

                        if (((RubyTimeFormatToken.Directive)token).getFormatDirective() ==
                                RubyTimeFormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY) {
                            builder.setWeekOfYearStartingWithSunday((int)week);
                        } else {
                            builder.setWeekOfYearStartingWithMonday((int)week);
                        }
                        break;
                    }
                    case DAY_OF_WEEK_STARTING_WITH_MONDAY_1: { // %u, %Ou - Day of the week (Monday is 1, 1..7)
                        final long day = readDigits(1);
                        if (!validRange(day, 1, 7)) {
                            fail = true;
                        }
                        builder.setDayOfWeekStartingWithMonday1((int)day);
                        break;
                    }
                    case WEEK_OF_WEEK_BASED_YEAR: { // %V, %OV - Week number of the week-based year (01..53)
                        final long week = readDigits(2);
                        if (!validRange(week, 1, 53)) {
                            fail = true;
                        }
                        builder.setWeekOfWeekBasedYear((int)week);
                        break;
                    }
                    case DAY_OF_WEEK_STARTING_WITH_SUNDAY_0: { // %w - Day of the week (Sunday is 0, 0..6)
                        final long day = readDigits(1);
                        if (!validRange(day, 0, 6)) {
                            fail = true;
                        }
                        builder.setDayOfWeekStartingWithSunday0((int)day);
                        break;
                    }
                    case YEAR_WITH_CENTURY: {
                        // %Y, %EY - Year with century (can be negative, 4 digits at least)
                        //           -0001, 0000, 1995, 2009, 14292, etc.
                        boolean negative = false;
                        if (isSign(text, pos)) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        final long year;
                        if (isNumberPattern(tokenWithNext.getNextToken())) {
                            year = readDigits(4);
                        } else {
                            year = readDigitsMax();
                        }

                        builder.setYear((int)(!negative ? year : -year));
                        break;
                    }
                    case YEAR_WITHOUT_CENTURY: { // %y, %Ey, %Oy - year % 100 (00..99)
                        final long y = readDigits(2);
                        if (!validRange(y, 0, 99)) {
                            fail = true;
                        }
                        builder.setYearWithoutCentury((int)y);
                        break;
                    }
                    case TIME_ZONE_NAME: // %Z - Time zone abbreviation name
                    case TIME_OFFSET: {
                        // %z - Time zone as hour and minute offset from UTC (e.g. +0900)
                        //      %:z - hour and minute offset from UTC with a colon (e.g. +09:00)
                        //      %::z - hour, minute and second offset from UTC (e.g. +09:00:00)
                        //      %:::z - hour, minute and second offset from UTC
                        //          (e.g. +09, +09:30, +09:30:30)
                        if (isEndOfText(text, pos)) {
                            fail = true;
                            break;
                        }

                        final Matcher m = ZONE_PARSE_REGEX.matcher(text.substring(pos));
                        if (m.find()) {
                            // zone
                            String zone = text.substring(pos, pos + m.end());
                            builder.setTimeOffset(zone);
                            pos += zone.length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    }
                }
            }

            if (fail) {
                return null;
            }

            if (text.length() > pos) {
                builder.setLeftover(text.substring(pos, text.length()));
            }

            return builder.build();
        }

        /**
         * The method is reimplemented based on read_digits from Ruby v2.3.1's ext/date/date_strptime.c.
         *
         * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l77">read_digits</a>
         */
        private long readDigits(final int len)
        {
            char c;
            long v = 0;
            final int initPos = pos;

            for (int i = 0; i < len; i++) {
                if (isEndOfText(text, pos)) {
                    break;
                }

                c = text.charAt(pos);
                if (!isDigit(c)) {
                    break;
                }
                else {
                    v = v * 10 + toInt(c);
                }
                pos += 1;
            }

            if (pos == initPos) {
                fail = true;
            }

            return v;
        }

        /**
         * The method is reimplemented based on READ_DIGITS_MAX from Ruby v2.3.1's ext/date/date_strptime.c.
         *
         * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l137">READ_DIGITS_MAX</a>
         */
        private long readDigitsMax()
        {
            return readDigits(Integer.MAX_VALUE);
        }

        /**
         * Returns -1 if text doesn't match with patterns.
         */
        private int findIndexInPatterns(final String[] patterns)
        {
            if (isEndOfText(text, pos)) {
                return -1;
            }

            for (int i = 0; i < patterns.length; i++) {
                final String pattern = patterns[i];
                final int len = pattern.length();
                if (!isEndOfText(text, pos + len - 1)
                        && pattern.equalsIgnoreCase(text.substring(pos, pos + len))) { // strncasecmp
                    return i;
                }
            }

            return -1; // text doesn't match at any patterns.
        }

        /**
         * The method is reimplemented based on num_pattern_p from Ruby v2.3.1's ext/date/date_strptime.c.
         *
         * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l58">num_pattern_p</a>
         */
        private static boolean isNumberPattern(final RubyTimeFormatToken token)
        {
            if (token == null) {
                return false;
            }
            else if ((!token.isDirective()) && isDigit(((RubyTimeFormatToken.Immediate)token).getContent().charAt(0))) {
                return true;
            }
            else if (token.isDirective() && ((RubyTimeFormatToken.Directive)token).getFormatDirective().isNumeric()) {
                return true;
            }
            else {
                return false;
            }
        }

        /**
         * The method is reimplemented based on valid_range_p from Ruby v2.3.1's ext/date/date_strptime.c.
         *
         * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l
139">valid_range_p</a>
         */
        private static boolean validRange(long v, int lower, int upper)
        {
            return lower <= v && v <= upper;
        }

        private static boolean isSpace(char c)
        {
            return c == ' ' || c == '\t' || c == '\n' ||
                    c == '\u000b' || c == '\f' || c == '\r';
        }

        private static boolean isDigit(char c)
        {
            return '0' <= c && c <= '9';
        }

        private static boolean isEndOfText(String text, int pos)
        {
            return pos >= text.length();
        }

        private static boolean isSign(String text, int pos)
        {
            return !isEndOfText(text, pos) && (text.charAt(pos) == '+' || text.charAt(pos) == '-');
        }

        private static boolean isMinus(String text, int pos)
        {
            return !isEndOfText(text, pos) && text.charAt(pos) == '-';
        }

        private static boolean isBlank(String text, int pos)
        {
            return !isEndOfText(text, pos) && text.charAt(pos) == ' ';
        }

        private static int toInt(char c)
        {
            return c - '0';
        }
    }

    private final RubyTimeFormat format;
}
