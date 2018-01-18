package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * TestRubyTimeFormat tests org.embulk.spi.time.RubyTimeFormat.
 */
public class TestRubyTimeFormat {
    @Test
    public void testEmpty() {
        testFormat("");
    }

    @Test
    public void testSingles() {
        testFormat("%Y", RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens());
        testFormat("%C", RubyTimeFormatDirective.CENTURY.toTokens());
        testFormat("%y", RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());

        testFormat("%m", RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens());
        testFormat("%B", RubyTimeFormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens());
        testFormat("%b", RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens());
        testFormat("%h", RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H.toTokens());

        testFormat("%d", RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%e", RubyTimeFormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens());

        testFormat("%j", RubyTimeFormatDirective.DAY_OF_YEAR.toTokens());

        testFormat("%H", RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens());
        testFormat("%k", RubyTimeFormatDirective.HOUR_OF_DAY_BLANK_PADDED.toTokens());
        testFormat("%I", RubyTimeFormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens());
        testFormat("%l", RubyTimeFormatDirective.HOUR_OF_AMPM_BLANK_PADDED.toTokens());
        testFormat("%P", RubyTimeFormatDirective.AMPM_OF_DAY_LOWER_CASE.toTokens());
        testFormat("%p", RubyTimeFormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens());

        testFormat("%M", RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens());

        testFormat("%S", RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());

        testFormat("%L", RubyTimeFormatDirective.MILLI_OF_SECOND.toTokens());
        testFormat("%N", RubyTimeFormatDirective.NANO_OF_SECOND.toTokens());

        testFormat("%z", RubyTimeFormatDirective.TIME_OFFSET.toTokens());
        testFormat("%Z", RubyTimeFormatDirective.TIME_ZONE_NAME.toTokens());

        testFormat("%A", RubyTimeFormatDirective.DAY_OF_WEEK_FULL_NAME.toTokens());
        testFormat("%a", RubyTimeFormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens());
        testFormat("%u", RubyTimeFormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1.toTokens());
        testFormat("%w", RubyTimeFormatDirective.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0.toTokens());

        testFormat("%G", RubyTimeFormatDirective.WEEK_BASED_YEAR_WITH_CENTURY.toTokens());
        testFormat("%g", RubyTimeFormatDirective.WEEK_BASED_YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%V", RubyTimeFormatDirective.WEEK_OF_WEEK_BASED_YEAR.toTokens());

        testFormat("%U", RubyTimeFormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY.toTokens());
        testFormat("%W", RubyTimeFormatDirective.WEEK_OF_YEAR_STARTING_WITH_MONDAY.toTokens());

        testFormat("%s", RubyTimeFormatDirective.SECOND_SINCE_EPOCH.toTokens());
        testFormat("%Q", RubyTimeFormatDirective.MILLISECOND_SINCE_EPOCH.toTokens());
    }

    @Test
    public void testRecurred() {
        testFormat("%c",
                   RubyTimeFormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens());
        testFormat("%D",
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('/'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('/'),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%x",
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('/'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('/'),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%F",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%n",
                   new RubyTimeFormatToken.Immediate('\n'));
        testFormat("%R",
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens());
        testFormat("%r",
                   RubyTimeFormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens());
        testFormat("%T",
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%X",
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%t",
                   new RubyTimeFormatToken.Immediate('\t'));
        testFormat("%v",
                   RubyTimeFormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens());
        testFormat("%+",
                   RubyTimeFormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.TIME_ZONE_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens());
    }

    @Test
    public void testExtended() {
        testFormat("%EC", RubyTimeFormatDirective.CENTURY.toTokens());
        testFormat("%Oy", RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%:z", RubyTimeFormatDirective.TIME_OFFSET.toTokens());
        testFormat("%::z", RubyTimeFormatDirective.TIME_OFFSET.toTokens());
        testFormat("%:::z", RubyTimeFormatDirective.TIME_OFFSET.toTokens());
    }

    @Test
    public void testPercents() {
        testFormat("%", new RubyTimeFormatToken.Immediate('%'));
        testFormat("%%", new RubyTimeFormatToken.Immediate('%'));

        // Split into two "%" tokens for some internal reasons.
        testFormat("%%%", new RubyTimeFormatToken.Immediate('%'), new RubyTimeFormatToken.Immediate('%'));
        testFormat("%%%%", new RubyTimeFormatToken.Immediate('%'), new RubyTimeFormatToken.Immediate('%'));
    }

    @Test
    public void testOrdinary() {
        testFormat("abc123", new RubyTimeFormatToken.Immediate("abc123"));
    }

    @Test
    public void testPercentButOrdinary() {
        testFormat("%f", new RubyTimeFormatToken.Immediate("%f"));
        testFormat("%Ed", new RubyTimeFormatToken.Immediate("%Ed"));
        testFormat("%OY", new RubyTimeFormatToken.Immediate("%OY"));
        testFormat("%::::z", new RubyTimeFormatToken.Immediate("%::::z"));
    }

    @Test
    public void testSpecifiersAndOrdinary() {
        testFormat("ab%Out%Expose",
                   new RubyTimeFormatToken.Immediate("ab"),
                   RubyTimeFormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1.toTokens(),
                   new RubyTimeFormatToken.Immediate("t"),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('/'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('/'),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate("pose"));
    }

    @Test
    public void testRubyTestPatterns() {
        testFormat("%Y-%m-%dT%H:%M:%S",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('T'),
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%d-%b-%y",
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%A %B %d %y",
                   RubyTimeFormatDirective.DAY_OF_WEEK_FULL_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%B %d, %y",
                   RubyTimeFormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(", "),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%B%t%d,%n%y",
                   RubyTimeFormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate('\t'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(','),
                   new RubyTimeFormatToken.Immediate('\n'),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%I:%M:%S %p",
                   RubyTimeFormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens());
        testFormat("%I:%M:%S %p %Z",
                   RubyTimeFormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens(),
                   new RubyTimeFormatToken.Immediate(' '),
                   RubyTimeFormatDirective.TIME_ZONE_NAME.toTokens());
        testFormat("%a%d%b%y%H%p%Z",
                   RubyTimeFormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens(),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   RubyTimeFormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   RubyTimeFormatDirective.YEAR_WITHOUT_CENTURY.toTokens(),
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   RubyTimeFormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens(),
                   RubyTimeFormatDirective.TIME_ZONE_NAME.toTokens());
        testFormat("%Y9%m9%d",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('9'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('9'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%k%M%S",
                   RubyTimeFormatDirective.HOUR_OF_DAY_BLANK_PADDED.toTokens(),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%l%M%S",
                   RubyTimeFormatDirective.HOUR_OF_AMPM_BLANK_PADDED.toTokens(),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%Y.",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('.'));
        testFormat("%Y. ",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate(". "));
        testFormat("%Y-%m-%d",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%Y-%m-%e",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens());
        testFormat("%Y-%j",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_YEAR.toTokens());
        testFormat("%H:%M:%S",
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%k:%M:%S",
                   RubyTimeFormatDirective.HOUR_OF_DAY_BLANK_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%A,",
                   RubyTimeFormatDirective.DAY_OF_WEEK_FULL_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(','));
        testFormat("%B,",
                   RubyTimeFormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new RubyTimeFormatToken.Immediate(','));
        testFormat("%FT%T%Z",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('T'),
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   RubyTimeFormatDirective.TIME_ZONE_NAME.toTokens());
        testFormat("%FT%T.%N%Z",
                   RubyTimeFormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.MONTH_OF_YEAR.toTokens(),
                   new RubyTimeFormatToken.Immediate('-'),
                   RubyTimeFormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate('T'),
                   RubyTimeFormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new RubyTimeFormatToken.Immediate(':'),
                   RubyTimeFormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new RubyTimeFormatToken.Immediate('.'),
                   RubyTimeFormatDirective.NANO_OF_SECOND.toTokens(),
                   RubyTimeFormatDirective.TIME_ZONE_NAME.toTokens());
    }

    private void testFormat(final String formatString, final Object... expectedTokensInArray) {
        final List<RubyTimeFormatToken> expectedTokens = new ArrayList<>();
        for (final Object expectedElement : expectedTokensInArray) {
            if (expectedElement instanceof RubyTimeFormatToken) {
                expectedTokens.add((RubyTimeFormatToken) expectedElement);
            } else if (expectedElement instanceof List) {
                for (final Object expectedElement2 : (List) expectedElement) {
                    if (expectedElement2 instanceof RubyTimeFormatToken) {
                        expectedTokens.add((RubyTimeFormatToken) expectedElement2);
                    } else {
                        fail();
                    }
                }
            } else {
                fail();
            }
        }
        final RubyTimeFormat expectedFormat = RubyTimeFormat.createForTesting(expectedTokens);
        final RubyTimeFormat actualFormat = RubyTimeFormat.compile(formatString);
        assertEquals(expectedFormat, actualFormat);
    }
}
