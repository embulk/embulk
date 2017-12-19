package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * TestTimestampParser tests org.embulk.spi.time.TimestampParser.
 *
 * Some test cases are imported from Ruby v2.3.1's test/date/test_date_strptime.rb. See its COPYING for license.
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/test/date/test_date_strptime.rb?view=markup">test/date/test_date_strptime.rb</a>
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup">COPYING</a>
 */
public class TestTimestampParser {
    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_iso8601() {
        testToParse("2001-02-03", "%Y-%m-%d", 981158400L);
        testToParse("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S", 981244799L);
        testToParse("2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 981212399L);
        testToParse("-2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -125309754001L);
        testToParse("+012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 327406287599L);
        testToParse("-012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -451734829201L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_ctime3_asctime3() {
        testToParse("Thu Jul 29 14:47:19 1999", "%c", 933259639L);
        testToParse("Thu Jul 29 14:47:19 -1999", "%c", -125231389961L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_date1() {
        testToParse("Thu Jul 29 16:39:41 EST 1999", "%a %b %d %H:%M:%S %Z %Y", 933284381L);
        testToParse("Thu Jul 29 16:39:41 MET DST 1999", "%a %b %d %H:%M:%S %Z %Y", 933259181L);
        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "AST" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        testToParse("Thu Jul 29 16:39:41 AST 1999", "%a %b %d %H:%M:%S %Z %Y", 933280781L);
        testToParse("Thu Jul 29 16:39:41 AST -1999", "%a %b %d %H:%M:%S %Z %Y", -125231368819L);
        testToParse("Thu Jul 29 16:39:41 GMT+09 1999", "%a %b %d %H:%M:%S %Z %Y", 933233981L);
        testToParse("Thu Jul 29 16:39:41 GMT+0908 1999", "%a %b %d %H:%M:%S %Z %Y", 933233501L);
        testToParse("Thu Jul 29 16:39:41 GMT+090807 1999", "%a %b %d %H:%M:%S %Z %Y", 933233494L);
        testToParse("Thu Jul 29 16:39:41 GMT-09 1999", "%a %b %d %H:%M:%S %Z %Y", 933298781L);
        testToParse("Thu Jul 29 16:39:41 GMT-09:08 1999", "%a %b %d %H:%M:%S %Z %Y", 933299261L);
        testToParse("Thu Jul 29 16:39:41 GMT-09:08:07 1999", "%a %b %d %H:%M:%S %Z %Y", 933299268L);
        testToParse("Thu Jul 29 16:39:41 GMT-3.5 1999", "%a %b %d %H:%M:%S %Z %Y", 933278981L);
        testToParse("Thu Jul 29 16:39:41 GMT-3,5 1999", "%a %b %d %H:%M:%S %Z %Y", 933278981L);
        testToParse("Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933287981L);
        testToParse("Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933230381L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_rfc822() {
        testToParse("Thu, 29 Jul 1999 09:54:21 UT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        testToParse("Thu, 29 Jul 1999 09:54:21 GMT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);

        // It should be 933267261L if PDT (Pacific Daylight Time) is correctly -07:00.
        //
        // Joda-Time's DateTimeFormat.forPattern("z").parseMillis("PDT") however returns 8 hours (-08:00).
        // DateTimeFormat.forPattern("z").parseMillis("PDT") == 28800000
        // https://github.com/JodaOrg/joda-time/blob/v2.9.2/src/main/java/org/joda/time/DateTimeUtils.java#L446
        //
        // Embulk has used it to parse time zones for a very long time since it was v0.1.
        // https://github.com/embulk/embulk/commit/b97954a5c78397e1269bbb6979d6225dfceb4e05
        //
        // It is kept as -08:00 for compatibility as of now.
        //
        // TODO: Make time zone parsing consistent.
        // @see <a href="https://github.com/embulk/embulk/issues/860">https://github.com/embulk/embulk/issues/860</a>
        testToParse("Thu, 29 Jul 1999 09:54:21 PDT", "%a, %d %b %Y %H:%M:%S %Z", 933270861L);

        testToParse("Thu, 29 Jul 1999 09:54:21 z", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        testToParse("Thu, 29 Jul 1999 09:54:21 +0900", "%a, %d %b %Y %H:%M:%S %Z", 933209661L);
        testToParse("Thu, 29 Jul 1999 09:54:21 +0430", "%a, %d %b %Y %H:%M:%S %Z", 933225861L);
        testToParse("Thu, 29 Jul 1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", 933258261L);
        testToParse("Thu, 29 Jul -1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", -125231391339L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_etc() {
        testToParse("06-DEC-99", "%d-%b-%y", 944438400L);
        testToParse("sUnDay oCtoBer 31 01", "%A %B %d %y", 1004486400L);
        // Their "\u000b" are actually "\v" in Ruby v2.3.1's tests. "\v" is not recognized as a character in Java.
        testToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B %d, %y", 939945600L);
        testToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B%t%d,%n%y", 939945600L);

        testToParse("09:02:11 AM", "%I:%M:%S %p", 81955357331L);
        testToParse("09:02:11 A.M.", "%I:%M:%S %p", 81955357331L);
        testToParse("09:02:11 PM", "%I:%M:%S %p", 81955400531L);
        testToParse("09:02:11 P.M.", "%I:%M:%S %p", 81955400531L);

        testToParse("12:33:44 AM", "%r", 81955326824L);
        testToParse("01:33:44 AM", "%r", 81955330424L);
        testToParse("11:33:44 AM", "%r", 81955366424L);
        testToParse("12:33:44 PM", "%r", 81955370024L);
        testToParse("01:33:44 PM", "%r", 81955373624L);
        testToParse("11:33:44 PM", "%r", 81955409624L);

        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "AST" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        testToParse("11:33:44 PM AST", "%I:%M:%S %p %Z", 81955424024L);
        testToParse("11:33:44 P.M. AST", "%I:%M:%S %p %Z", 81955424024L);

        testToParse("fri1feb034pm+5", "%a%d%b%y%H%p%Z", 1044097200L);
    }

    @Test  // Imported from test__strptime__width in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__width() {
        testToParse("99", "%y", 917049600L);
        testToParse("01", "%y", 980208000L);
        testToParse("19 99", "%C %y", 917049600L);
        testToParse("20 01", "%C %y", 980208000L);
        testToParse("30 99", "%C %y", 35629718400L);
        testToParse("30 01", "%C %y", 32537116800L);
        testToParse("1999", "%C%y", 917049600L);
        testToParse("2001", "%C%y", 980208000L);
        testToParse("3099", "%C%y", 35629718400L);
        testToParse("3001", "%C%y", 32537116800L);

        testToParse("20060806", "%Y", 632995726752000L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testToParse("20060806", "%Y ", 632995726752000L);
        testToParse("20060806", "%Y%m%d", 1154822400L);
        testToParse("2006908906", "%Y9%m9%d", 1154822400L);
        testToParse("12006 08 06", "%Y %m %d", 316724342400L);
        testToParse("12006-08-06", "%Y-%m-%d", 316724342400L);
        testToParse("200608 6", "%Y%m%e", 1154822400L);

        testToParse("2006333", "%Y%j", 1164758400L);
        testToParse("20069333", "%Y9%j", 1164758400L);
        testToParse("12006 333", "%Y %j", 316734278400L);
        testToParse("12006-333", "%Y-%j", 316734278400L);

        testToParse("232425", "%H%M%S", 81955409065L);
        testToParse("23924925", "%H9%M9%S", 81955409065L);
        testToParse("23 24 25", "%H %M %S", 81955409065L);
        testToParse("23:24:25", "%H:%M:%S", 81955409065L);
        testToParse(" 32425", "%k%M%S", 81955337065L);
        testToParse(" 32425", "%l%M%S", 81955337065L);

        // They are intentionally skipped as a month and a day of week are not sufficient to build a timestamp.
        // [['FriAug', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FriAug', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
    }

    @Test  // Imported from test__strptime__fail in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__fail() {
        testToParse("2001.", "%Y.", 980208000L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testToParse("2001. ", "%Y.", 980208000L);
        testToParse("2001.", "%Y. ", 980208000L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testToParse("2001. ", "%Y. ", 980208000L);

        failToParse("2001", "%Y.");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        failToParse("2001 ", "%Y.");
        failToParse("2001", "%Y. ");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        failToParse("2001 ", "%Y. ");

        failToParse("2001-13-31", "%Y-%m-%d");
        failToParse("2001-12-00", "%Y-%m-%d");
        failToParse("2001-12-32", "%Y-%m-%d");
        failToParse("2001-12-00", "%Y-%m-%e");
        failToParse("2001-12-32", "%Y-%m-%e");
        failToParse("2001-12-31", "%y-%m-%d");

        failToParse("2004-000", "%Y-%j");
        failToParse("2004-367", "%Y-%j");
        failToParse("2004-366", "%y-%j");

        testToParse("24:59:59", "%H:%M:%S", 81955414799L);
        testToParse("24:59:59", "%k:%M:%S", 81955414799L);
        testToParse("24:59:60", "%H:%M:%S", 81955414799L);
        testToParse("24:59:60", "%k:%M:%S", 81955414799L);

        failToParse("24:60:59", "%H:%M:%S");
        failToParse("24:60:59", "%k:%M:%S");
        failToParse("24:59:61", "%H:%M:%S");
        failToParse("24:59:61", "%k:%M:%S");
        failToParse("00:59:59", "%I:%M:%S");
        failToParse("13:59:59", "%I:%M:%S");
        failToParse("00:59:59", "%l:%M:%S");
        failToParse("13:59:59", "%l:%M:%S");

        testToParse("0", "%U", 81955324800L);
        failToParse("54", "%U");
        testToParse("0", "%W", 81955324800L);
        failToParse("54", "%W");
        failToParse("0", "%V");
        failToParse("54", "%V");
        failToParse("0", "%u");
        testToParse("7", "%u", 81955324800L);
        testToParse("0", "%w", 81955324800L);
        failToParse("7", "%w");

        failToParse("Sanday", "%A");
        failToParse("Jenuary", "%B");
        testToParse("Sundai", "%A", 81955324800L);
        testToParse("Januari", "%B", 81955324800L);
        failToParse("Sundai,", "%A,");
        failToParse("Januari,", "%B,");
    }

    @Test  // Imported partially from test_strptime in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test_strptime() {
        testToParse("2002-03-14T11:22:33Z", "%Y-%m-%dT%H:%M:%S%Z", 1016104953L);
        testToParse("2002-03-14T11:22:33+09:00", "%Y-%m-%dT%H:%M:%S%Z", 1016072553L);
        testToParse("2002-03-14T11:22:33-09:00", "%FT%T%Z", 1016137353L);
        testToParse("2002-03-14T11:22:33.123456789-09:00", "%FT%T.%N%Z", 1016137353L, 123456789);
    }

    @Test  // Imported from test_strptime__minus in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test_strptime__minus() {
        testToParse("-1", "%s", -1L);
        testToParse("-86400", "%s", -86400L);

        // In |java.time.Instant|, it is always 0 <= nanoAdjustment < 1,000,000,000.
        // -0.9s is represented like -1s + 100ms.
        testToParse("-999", "%Q", -1L, 1000000);
        testToParse("-1000", "%Q", -1L);
    }

    private void testToParse(final String string, final String format, final long second, final int nanoOfSecond) {
        final TimestampParser parser = new TimestampParser(format, DateTimeZone.UTC, "4567-01-23");
        final Timestamp timestamp = parser.parse(string);
        assertEquals(second, timestamp.getEpochSecond());
        assertEquals(nanoOfSecond,timestamp.getNano());
    }

    private void testToParse(final String string, final String format, final long second) {
        testToParse(string, format, second, 0);
    }

    private void failToParse(final String string, final String format) {
        final TimestampParser parser = new TimestampParser(format, DateTimeZone.UTC, "4567-01-23");
        try {
            parser.parse(string);
        } catch (TimestampParseException ex) {
            return;
        }
        fail();
    }
}
