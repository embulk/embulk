package org.embulk.spi.time;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RubyTimeParsed is TimeParsed from Ruby-style date/time formats.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 *
 * A part of this class is reimplementation of Ruby v2.3.1's lib/time.rb. See its COPYING for license.
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup">lib/time.rb</a>
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup">COPYING</a>
 */
class RubyTimeParsed extends TimeParsed {
    // TODO: Make it private once LegacyRubyTimeParsed is removed.
    RubyTimeParsed(
            final String originalString,

            final int dayOfMonth,
            final int weekBasedYear,
            final int hour,
            final int dayOfYear,
            final int nanoOfSecond,
            final int minuteOfHour,
            final int monthOfYear,
            final Instant instantSeconds,
            final int secondOfMinute,
            final int weekOfYearStartingWithSunday,
            final int weekOfYearStartingWithMonday,
            final int dayOfWeekStartingWithMonday1,
            final int weekOfWeekBasedYear,
            final int dayOfWeekStartingWithSunday0,
            final int year,

            final String timeZoneName,

            final String leftover) {
        this.originalString = originalString;

        this.dayOfMonth = dayOfMonth;
        this.weekBasedYear = weekBasedYear;
        this.hour = hour;
        this.dayOfYear = dayOfYear;
        this.nanoOfSecond = nanoOfSecond;
        this.minuteOfHour = minuteOfHour;
        this.monthOfYear = monthOfYear;
        this.instantSeconds = instantSeconds;
        this.secondOfMinute = secondOfMinute;
        this.weekOfYearStartingWithSunday = weekOfYearStartingWithSunday;
        this.weekOfYearStartingWithMonday = weekOfYearStartingWithMonday;
        this.dayOfWeekStartingWithMonday1 = dayOfWeekStartingWithMonday1;
        this.weekOfWeekBasedYear = weekOfWeekBasedYear;
        this.dayOfWeekStartingWithSunday0 = dayOfWeekStartingWithSunday0;
        this.year = year;

        this.timeZoneName = timeZoneName;

        this.leftover = leftover;
    }

    static class Builder {
        Builder(final String originalString) {
            this.originalString = originalString;

            this.century = Integer.MIN_VALUE;
            this.dayOfMonth = Integer.MIN_VALUE;
            this.weekBasedYear = Integer.MIN_VALUE;
            this.hour = Integer.MIN_VALUE;
            this.dayOfYear = Integer.MIN_VALUE;
            this.nanoOfSecond = Integer.MIN_VALUE;
            this.minuteOfHour = Integer.MIN_VALUE;
            this.monthOfYear = Integer.MIN_VALUE;
            this.ampmOfDay = Integer.MIN_VALUE;
            this.instantSeconds = null;
            this.secondOfMinute = Integer.MIN_VALUE;
            this.weekOfYearStartingWithSunday = Integer.MIN_VALUE;
            this.weekOfYearStartingWithMonday = Integer.MIN_VALUE;
            this.dayOfWeekStartingWithMonday1 = Integer.MIN_VALUE;
            this.weekOfWeekBasedYear = Integer.MIN_VALUE;
            this.dayOfWeekStartingWithSunday0 = Integer.MIN_VALUE;
            this.year = Integer.MIN_VALUE;

            this.timeZoneName = null;

            this.leftover = null;

            this.fail = false;
        }

        RubyTimeParsed build() {
            // Merge week-based year and century as MRI (Matz' Ruby Implementation) does before generating a hash.
            // See: https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l676
            final int weekBasedYearWithCentury;
            if (this.century != Integer.MIN_VALUE && this.weekBasedYear != Integer.MIN_VALUE) {
                // It is the right behavior in Ruby.
                // Date._strptime('13 1234', '%C %G') => {:cwyear=>2534}
                weekBasedYearWithCentury = this.century * 100 + this.weekBasedYear;
            } else {
                weekBasedYearWithCentury = this.weekBasedYear;
            }

            // Merge year and century as MRI (Matz' Ruby Implementation) does before generating a hash.
            // See: https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l679
            final int yearWithCentury;
            if (this.century != Integer.MIN_VALUE && this.year != Integer.MIN_VALUE) {
                // It is the right behavior in Ruby.
                // Date._strptime('13 1234', '%C %Y') => {:year=>2534}
                yearWithCentury = this.century * 100 + this.year;
            } else {
                yearWithCentury = this.year;
            }

            // Merge hour and ampmOfDay as MRI (Matz' Ruby Implementation) does before generating a hash.
            // See: https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l685
            final int hourWithAmPm;
            if (this.hour != Integer.MIN_VALUE && this.ampmOfDay != Integer.MIN_VALUE) {
                hourWithAmPm = (this.hour % 12) + this.ampmOfDay;
            } else {
                hourWithAmPm = this.hour;
            }

            return new RubyTimeParsed(
                    this.originalString,

                    this.dayOfMonth,
                    weekBasedYearWithCentury,
                    hourWithAmPm,
                    this.dayOfYear,
                    this.nanoOfSecond,
                    this.minuteOfHour,
                    this.monthOfYear,
                    this.instantSeconds,
                    this.secondOfMinute,
                    this.weekOfYearStartingWithSunday,
                    this.weekOfYearStartingWithMonday,
                    this.dayOfWeekStartingWithMonday1,
                    this.weekOfWeekBasedYear,
                    this.dayOfWeekStartingWithSunday0,
                    yearWithCentury,

                    this.timeZoneName,

                    this.leftover);
        }

        /**
         * Sets day of the week by name.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wday
         * <li> Ruby strptime directive specifier related: %A, %a
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setDayOfWeekByName(final String dayOfWeekByName) {
            final Integer dayOfWeek = DAY_OF_WEEK_NAMES.get(dayOfWeekByName);
            if (dayOfWeek == null) {
                fail = true;
            } else {
                this.dayOfWeekStartingWithSunday0 = dayOfWeek;
            }
            return this;
        }

        /**
         * Sets month of the year by name.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: mon
         * <li> Ruby strptime directive specifier related: %B, %b, %h
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setMonthOfYearByName(final String monthOfYearByName) {
            final Integer monthOfYear = MONTH_OF_YEAR_NAMES.get(monthOfYearByName);
            if (monthOfYear == null) {
                fail = true;
            } else {
                this.monthOfYear = monthOfYear;
            }
            return this;
        }

        /**
         * Sets century.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: _cent
         * <li> Ruby strptime directive specifier related: %C
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setCentury(final int century) {
            this.century = century;
            return this;
        }

        /**
         * Sets day of the month.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: mday
         * <li> Ruby strptime directive specifier related: %d, %e
         * <li> java.time.temporal: ChronoField.DAY_OF_MONTH
         * </ul>
         */
        Builder setDayOfMonth(final int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }

        /**
         * Sets week-based year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cwyear
         * <li> Ruby strptime directive specifier related: %G
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setWeekBasedYear(final int weekBasedYear) {
            this.weekBasedYear = weekBasedYear;
            return this;
        }

        /**
         * Sets week-based year without century.
         *
         * If century is not set before by setCentury (%C), it tries to set default century as well.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cwyear
         * <li> Ruby strptime directive specifier related: %g
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setWeekBasedYearWithoutCentury(final int weekBasedYearWithoutCentury) {
            this.weekBasedYear = weekBasedYearWithoutCentury;
            if (this.century == Integer.MIN_VALUE) {
                this.century = (weekBasedYearWithoutCentury >= 69 ? 19 : 20);
            }
            return this;
        }

        /**
         * Sets hour.
         *
         * The hour can be either in 12-hour or 24-hour. It is considered 12-hour if setAmPmOfDay (%P/%p) is set.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: hour
         * <li> Ruby strptime directive specifier related: %H, %k, %I, %l
         * <li> java.time.temporal: ChronoField.HOUR_OF_AMPM or ChronoField.HOUR_OF_DAY
         * </ul>
         */
        Builder setHour(final int hour) {
            this.hour = hour;
            return this;
        }

        /**
         * Sets day of the year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: yday
         * <li> Ruby strptime directive specifier related: %j
         * <li> java.time.temporal: ChronoField.DAY_OF_YEAR
         * </ul>
         */
        Builder setDayOfYear(final int dayOfYear) {
            this.dayOfYear = dayOfYear;
            return this;
        }

        /**
         * Sets fractional part of the second.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: sec_fraction
         * <li> Ruby strptime directive specifier related: %L, %N
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setNanoOfSecond(final int nanoOfSecond) {
            this.nanoOfSecond = nanoOfSecond;
            return this;
        }

        /**
         * Sets minute of the hour.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: min
         * <li> Ruby strptime directive specifier related: %M
         * <li> java.time.temporal: ChronoField.MINUTE_OF_HOUR
         * </ul>
         */
        Builder setMinuteOfHour(final int minuteOfHour) {
            this.minuteOfHour = minuteOfHour;
            return this;
        }

        /**
         * Sets month of the year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: mon
         * <li> Ruby strptime directive specifier related: %m
         * <li> java.time.temporal: ChronoField.MONTH_OF_YEAR
         * </ul>
         */
        Builder setMonthOfYear(final int monthOfYear) {
            this.monthOfYear = monthOfYear;
            return this;
        }

        /**
         * Sets am/pm of the day.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: _merid
         * <li> Ruby strptime directive specifier related: %P, %p
         * <li> java.time.temporal: ChronoField.AMPM_OF_DAY
         * </ul>
         */
        Builder setAmPmOfDay(final int ampmOfDay) {
            this.ampmOfDay = ampmOfDay;
            return this;
        }

        /**
         * Sets second since the epoch.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: seconds
         * <li> Ruby strptime directive specifier related: %Q, %s
         * <li> java.time.temporal: ChronoField.INSTANT_SECONDS
         * </ul>
         */
        Builder setInstantSeconds(final Instant instantSeconds) {
            this.instantSeconds = instantSeconds;
            return this;
        }

        /**
         * Sets second of the minute.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: sec
         * <li> Ruby strptime directive specifier related: %S
         * <li> java.time.temporal: ChronoField.SECOND_OF_MINUTE
         * </ul>
         */
        Builder setSecondOfMinute(final int secondOfMinute) {
            this.secondOfMinute = secondOfMinute;
            return this;
        }

        /**
         * Sets week number with weeks starting from Sunday.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wnum0
         * <li> Ruby strptime directive specifier related: %U
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setWeekOfYearStartingWithSunday(final int weekOfYearStartingWithSunday) {
            this.weekOfYearStartingWithSunday = weekOfYearStartingWithSunday;
            return this;
        }

        /**
         * Sets week number with weeks starting from Monday.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wnum1
         * <li> Ruby strptime directive specifier related: %W
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setWeekOfYearStartingWithMonday(final int weekOfYearStartingWithMonday) {
            this.weekOfYearStartingWithMonday = weekOfYearStartingWithMonday;
            return this;
        }

        /**
         * Sets day of week starting from Monday with 1.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cwday
         * <li> Ruby strptime directive specifier related: %u
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setDayOfWeekStartingWithMonday1(final int dayOfWeekStartingWithMonday1) {
            this.dayOfWeekStartingWithMonday1 = dayOfWeekStartingWithMonday1;
            return this;
        }

        /**
         * Sets week number of week-based year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cweek
         * <li> Ruby strptime directive specifier related: %V
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setWeekOfWeekBasedYear(final int weekOfWeekBasedYear) {
            this.weekOfWeekBasedYear = weekOfWeekBasedYear;
            return this;
        }

        /**
         * Sets day of week starting from Sunday with 0.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wday
         * <li> Ruby strptime directive specifier related: %w
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setDayOfWeekStartingWithSunday0(final int dayOfWeekStartingWithSunday0) {
            this.dayOfWeekStartingWithSunday0 = dayOfWeekStartingWithSunday0;
            return this;
        }

        /**
         * Sets year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: year
         * <li> Ruby strptime directive specifier related: %Y
         * <li> java.time.temporal: ChronoField.YEAR
         * </ul>
         */
        Builder setYear(final int year) {
            this.year = year;
            return this;
        }

        /**
         * Sets year without century.
         *
         * If century is not set before by setCentury (%C), it tries to set default century as well.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: year
         * <li> Ruby strptime directive specifier related: %y
         * <li> java.time.temporal: ChronoField.YEAR
         * </ul>
         */
        Builder setYearWithoutCentury(final int yearWithoutCentury) {
            this.year = yearWithoutCentury;
            if (this.century == Integer.MIN_VALUE) {
                this.century = (yearWithoutCentury >= 69 ? 19 : 20);
            }
            return this;
        }

        /**
         * Sets time offset.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: zone, offset
         * <li> Ruby strptime directive specifier related: %Z, %z
         * <li> java.time.temporal: ChronoField.OFFSET_SECONDS (not exactly the same)
         * </ul>
         */
        Builder setTimeOffset(final String timeZoneName) {
            this.timeZoneName = timeZoneName;
            return this;
        }

        /**
         * Sets leftover.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: leftover
         * <li> Ruby strptime directive specifier related: N/A
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setLeftover(final String leftover) {
            this.leftover = leftover;
            return this;
        }

        private static final Map<String, Integer> DAY_OF_WEEK_NAMES;
        private static final Map<String, Integer> MONTH_OF_YEAR_NAMES;

        private static final String[] DAY_OF_WEEK_FULL_NAMES = new String[] {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };

        private static final String[] DAY_OF_WEEK_ABBREVIATED_NAMES = new String[] {
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
        };

        private static final String[] MONTH_OF_YEAR_FULL_NAMES = new String[] {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        private static final String[] MONTH_OF_YEAR_ABBREVIATED_NAMES = new String[] {
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };

        static {
            final HashMap<String, Integer> dayOfWeekNamesBuilt = new HashMap<>();
            for (int i = 0; i < DAY_OF_WEEK_FULL_NAMES.length; ++i) {
                dayOfWeekNamesBuilt.put(DAY_OF_WEEK_FULL_NAMES[i], i);
            }
            for (int i = 0; i < DAY_OF_WEEK_ABBREVIATED_NAMES.length; ++i) {
                dayOfWeekNamesBuilt.put(DAY_OF_WEEK_ABBREVIATED_NAMES[i], i);
            }
            DAY_OF_WEEK_NAMES = Collections.unmodifiableMap(dayOfWeekNamesBuilt);

            final HashMap<String, Integer> monthOfYearNamesBuilt = new HashMap<>();
            for (int i = 0; i < MONTH_OF_YEAR_FULL_NAMES.length; ++i) {
                monthOfYearNamesBuilt.put(MONTH_OF_YEAR_FULL_NAMES[i], i + 1);
            }
            for (int i = 0; i < MONTH_OF_YEAR_ABBREVIATED_NAMES.length; ++i) {
                monthOfYearNamesBuilt.put(MONTH_OF_YEAR_ABBREVIATED_NAMES[i], i + 1);
            }
            MONTH_OF_YEAR_NAMES = Collections.unmodifiableMap(monthOfYearNamesBuilt);
        }

        private final String originalString;

        private int century;
        private int dayOfMonth;
        private int weekBasedYear;
        private int hour;
        private int dayOfYear;
        private int nanoOfSecond;
        private int minuteOfHour;
        private int monthOfYear;
        private int ampmOfDay;
        private Instant instantSeconds;
        private int secondOfMinute;
        private int weekOfYearStartingWithSunday;
        private int weekOfYearStartingWithMonday;
        private int dayOfWeekStartingWithMonday1;
        private int weekOfWeekBasedYear;
        private int dayOfWeekStartingWithSunday0;
        private int year;

        private String timeZoneName;

        private String leftover;

        private boolean fail;
    }

    /**
     * Creates a java.time.Instant instance basically in the same way with Ruby v2.3.1's Time.strptime.
     *
     * The difference from Ruby v2.3.1's Time.strptime is that it does not consider "now" and local time zone.
     * If the given zone is neither numerical nor predefined textual time zones, it returns defaultZoneOffset then.
     *
     * The method is reimplemented based on strptime from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l427">strptime</a>
     */
    @Override
    Instant toInstant(final ZoneOffset defaultZoneOffset) {
        final ZoneOffset zoneOffset = TimeZoneIds.parseRubyTimeZoneOffset(this.timeZoneName, defaultZoneOffset);
        if (zoneOffset == null) {
            throw new TimestampParseException(
                    "Invalid time zone ID '" + this.timeZoneName + "' in '" + this.originalString + "'");
        }

        if (this.instantSeconds != null) {
            // Fractions by %Q are prioritized over fractions by %N.
            // irb(main):002:0> Time.strptime("123456789 12.345", "%Q %S.%N").nsec
            // => 789000000
            // irb(main):003:0> Time.strptime("12.345 123456789", "%S.%N %Q").nsec
            // => 789000000
            // irb(main):004:0> Time.strptime("12.345", "%S.%N").nsec
            // => 345000000
            if (!defaultZoneOffset.equals(ZoneOffset.UTC)) {
                // TODO: Warn that a default time zone is specified for epoch seconds.
            }
            if (this.timeZoneName != null) {
                // TODO: Warn that the epoch second has a time zone.
            }
            return this.instantSeconds;
        }

        // Day of the year (yday: DAY_OF_YEAR) is not considered in Time.strptime, not like DateTime.strptime.
        //
        // irb(main):002:0> Time.strptime("2001-128T23:59:59", "%Y-%jT%H:%M:%S")
        // => 2001-01-01 23:59:59 +0900
        // irb(main):004:0> DateTime.strptime("2001-128T23:59:59", "%Y-%jT%H:%M:%S")
        // => #<DateTime: 2001-05-08T23:59:59+00:00 ((2452038j,86399s,0n),+0s,2299161j)>

        final OffsetDateTime datetime = applyOffset(
                (this.year == Integer.MIN_VALUE ? 1970 : this.year),
                (this.monthOfYear == Integer.MIN_VALUE ? 1 : this.monthOfYear),
                (this.dayOfMonth == Integer.MIN_VALUE ? 1 : this.dayOfMonth),
                (this.hour == Integer.MIN_VALUE ? 0 : this.hour),
                (this.minuteOfHour == Integer.MIN_VALUE ? 0 : this.minuteOfHour),
                (this.secondOfMinute == Integer.MIN_VALUE ? 0 : this.secondOfMinute),
                (this.nanoOfSecond == Integer.MIN_VALUE ? 0 : this.nanoOfSecond),
                zoneOffset);

        return datetime.toInstant();
    }

    @Override
    Instant toInstant(final int defaultYear,
                      final int defaultMonthOfYear,
                      final int defaultDayOfMonth,
                      final ZoneId defaultZoneId) {
        // TODO: Implement it.
        throw new UnsupportedOperationException("Non-legacy RubyTimeParsed is not implemented.");
    }

    /**
     * Creates a java.time.Instant instance in legacy Embulk's way from this RubyTimeParsed instance with ZoneId.
     *
     * This method is to be called from LegacyRubyTimeParsed to access private fields.
     *
     * TODO: Remove this method once legacy Timestamp formats are removed.
     */
    final Instant toInstantLegacy(final int defaultYear,
                                  final int defaultMonthOfYear,
                                  final int defaultDayOfMonth,
                                  final ZoneId defaultZoneId) {
        if (this.instantSeconds != null) {
            // Fractions by %Q are prioritized over fractions by %N.
            // irb(main):002:0> Time.strptime("123456789 12.345", "%Q %S.%N").nsec
            // => 789000000
            // irb(main):003:0> Time.strptime("12.345 123456789", "%S.%N %Q").nsec
            // => 789000000
            // irb(main):004:0> Time.strptime("12.345", "%S.%N").nsec
            // => 345000000
            if (!defaultZoneId.equals(ZoneOffset.UTC)) {
                // TODO: Warn that a default time zone is specified for epoch seconds.
            }
            if (this.timeZoneName != null) {
                // TODO: Warn that the epoch second has a time zone.
            }
            return this.instantSeconds;
        }

        final ZoneId zoneId;
        if (this.timeZoneName != null) {
            zoneId = TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(this.timeZoneName);
            if (zoneId == null) {
                throw new TimestampParseException(
                        "Invalid time zone ID '" + this.timeZoneName + "' in '" + this.originalString + "'");
            }
        } else {
            zoneId = defaultZoneId;
        }

        // Leap seconds are considered as 59 when Ruby converts them to epochs.
        final int thisSecondOfMinute;
        if (this.secondOfMinute == Integer.MIN_VALUE) {
            thisSecondOfMinute = 0;
        } else if (this.secondOfMinute == 60) {
            thisSecondOfMinute = 59;
        } else {
            thisSecondOfMinute = this.secondOfMinute;
        }

        // TODO: Implement rolling over other than days if needed.
        final int daysRollover = (this.hour == Integer.MIN_VALUE ? 0 : this.hour / 24);

        final ZonedDateTime datetime;
        // yday is more prioritized than mon/mday in Ruby's strptime.
        if (this.dayOfYear != Integer.MIN_VALUE) {
            datetime = ZonedDateTime.of(
                    (this.year == Integer.MIN_VALUE ? defaultYear : this.year),
                    1,
                    1,
                    (this.hour == Integer.MIN_VALUE ? 0 : this.hour % 24),
                    (this.minuteOfHour == Integer.MIN_VALUE ? 0 : this.minuteOfHour),
                    thisSecondOfMinute,
                    (this.nanoOfSecond == Integer.MIN_VALUE ? 0 : this.nanoOfSecond),
                    zoneId).withDayOfYear(this.dayOfYear).plusDays(daysRollover);
        } else {
            datetime = ZonedDateTime.of(
                    (this.year == Integer.MIN_VALUE ? defaultYear : this.year),
                    (this.monthOfYear == Integer.MIN_VALUE ? defaultMonthOfYear : this.monthOfYear),
                    (this.dayOfMonth == Integer.MIN_VALUE ? defaultDayOfMonth : this.dayOfMonth),
                    (this.hour == Integer.MIN_VALUE ? 0 : this.hour % 24),
                    (this.minuteOfHour == Integer.MIN_VALUE ? 0 : this.minuteOfHour),
                    thisSecondOfMinute,
                    (this.nanoOfSecond == Integer.MIN_VALUE ? 0 : this.nanoOfSecond),
                    zoneId).plusDays(daysRollover);
        }
        return datetime.toInstant();
    }

    /**
     * Converts this RubyTimeParsed to LegacyRubyTimeParsed so that it can be converted to Instant in the legacy way.
     *
     * TODO: Remove this method once legacy Timestamp formats are removed.
     */
    final LegacyRubyTimeParsed toLegacy() {
        return new LegacyRubyTimeParsed(
                originalString,

                dayOfMonth,
                weekBasedYear,
                hour,
                dayOfYear,
                nanoOfSecond,
                minuteOfHour,
                monthOfYear,
                instantSeconds,
                secondOfMinute,
                weekOfYearStartingWithSunday,
                weekOfYearStartingWithMonday,
                dayOfWeekStartingWithMonday1,
                weekOfWeekBasedYear,
                dayOfWeekStartingWithSunday0,
                year,

                timeZoneName,

                leftover);
    }

    final Map<String, Object> asMapLikeRubyHash() {
        final HashMap<String, Object> hash = new HashMap<>();

        putIntIfValid(hash, "mday", this.dayOfMonth);
        putIntIfValid(hash, "cwyear", this.weekBasedYear);
        putIntIfValid(hash, "hour", this.hour);
        putIntIfValid(hash, "yday", this.dayOfYear);
        putFractionIfValid(hash, "sec_fraction", this.nanoOfSecond);
        putIntIfValid(hash, "min", this.minuteOfHour);
        putIntIfValid(hash, "mon", this.monthOfYear);
        putSecondWithFractionIfValid(hash, "seconds", this.instantSeconds);
        putIntIfValid(hash, "sec", this.secondOfMinute);
        putIntIfValid(hash, "wnum0", this.weekOfYearStartingWithSunday);
        putIntIfValid(hash, "wnum1", this.weekOfYearStartingWithMonday);
        putIntIfValid(hash, "cwday", this.dayOfWeekStartingWithMonday1);
        putIntIfValid(hash, "cweek", this.weekOfWeekBasedYear);
        putIntIfValid(hash, "wday", this.dayOfWeekStartingWithSunday0);
        putIntIfValid(hash, "year", this.year);
        putTimeZoneIfValid(hash, this.timeZoneName);
        putStringIfValid(hash, "leftover", this.leftover);

        return hash;
    }

    private int putIntIfValid(final Map<String, Object> hash, final String key, final int value) {
        if (value != Integer.MIN_VALUE) {
            hash.put(key, value);
        }
        return value;
    }

    private BigDecimal putFractionIfValid(final Map<String, Object> hash, final String key, final int value) {
        if (value != Integer.MIN_VALUE) {
            return (BigDecimal) hash.put(key, BigDecimal.ZERO.add(BigDecimal.valueOf(value, 9)));
        }
        return null;
    }

    private Object putSecondWithFractionIfValid(final Map<String, Object> hash, final String key, final Instant value) {
        if (value != null) {
            if (value.getNano() == 0) {
                return hash.put(key, value.getEpochSecond());
            } else {
                return hash.put(key,
                                BigDecimal.valueOf(value.getEpochSecond()).add(BigDecimal.valueOf(value.getNano(), 9)));
            }
        }
        return null;
    }

    private String putTimeZoneIfValid(final Map<String, Object> hash, final String timeZoneName) {
        if (timeZoneName != null) {
            final int offset = RubyTimeZoneTab.dateZoneToDiff(timeZoneName);
            if (offset != Integer.MIN_VALUE) {
                hash.put("offset", offset);
            }
            return (String) hash.put("zone", timeZoneName);
        }
        return timeZoneName;
    }

    private String putStringIfValid(final Map<String, Object> hash, final String key, final String value) {
        if (value != null) {
            return (String) hash.put(key, value);
        }
        return value;
    }

    /**
     * Applies time zone offset to date/time information, and creates java.time.OffsetDateTime in UTC.
     *
     * The method is reimplemented based on apply_offset from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l208">apply_offset</a>
     */
    private static OffsetDateTime applyOffset(int year,
                                              int monthOfYear,
                                              int dayOfMonth,
                                              int hourOfDay,
                                              int minuteOfHour,
                                              int secondOfMinute,
                                              final int nanoOfSecond,
                                              final ZoneOffset zoneOffset) throws TimestampParseException {
        int offset = zoneOffset.getTotalSeconds();

        // Processing leap seconds using time offsets in a bit tricky manner.
        //
        // Leap seconds are considered as the next second in Time.strptime.
        // irb(main):002:0> Time.strptime("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S")
        // => 2001-02-04 00:00:00 +0900
        if (secondOfMinute == 60) {
            secondOfMinute = 59;
            offset -= 1;
        }

        // Processing 24h in clock hours using time offsets in a bit tricky manner.
        //
        // 24h is considered as 0h in the next day in Time.strptime (if non-UTC time zone is specified).
        // irb(main):002:0> Time.strptime("24:59:59 PST", "%H:%M:%S %Z")
        // => 2018-01-13 00:59:59 -0800
        // irb(main):003:0> Time.strptime("24:59:59 UTC", "%H:%M:%S %Z")
        // ArgumentError: min out of range
        // ...
        // irb(main):004:0> Time.strptime("24:59:59", "%H:%M:%S")
        // ArgumentError: min out of range
        // ...
        //
        // 24h is always handled as 0h in the next day in Embulk as if non-UTC time zone is specified.
        if (hourOfDay == 24) {
            offset -= 24 * 60 * 60;
            hourOfDay = 0;
        }

        if (offset < 0) {
            offset = -offset;

            final int offsetSecond = offset % 60;
            offset = offset / 60;
            if (offsetSecond != 0) {
                secondOfMinute += offsetSecond;
                offset += secondOfMinute / 60;
                secondOfMinute %= 60;
            }

            final int offsetMinute = offset % 60;
            offset = offset / 60;
            if (offsetMinute != 0) {
                minuteOfHour += offsetMinute;
                offset += minuteOfHour / 60;
                minuteOfHour %= 60;
            }

            final int offsetHour = offset % 24;
            offset = offset / 24;
            if (offsetHour != 0) {
                hourOfDay += offsetHour;
                offset += hourOfDay / 24;
                hourOfDay %= 24;
            }

            if (offset != 0) {
                dayOfMonth += offset;
                final int days = monthDays(year, monthOfYear);
                if (days < dayOfMonth) {
                    monthOfYear += 1;
                    if (12 < monthOfYear) {
                        monthOfYear = 1;
                        year += 1;
                    }
                    dayOfMonth = 1;
                }
            }

        } else if (0 < offset) {
            final int offsetSecond = offset % 60;
            offset /= 60;
            if (offsetSecond != 0) {
                secondOfMinute -= offsetSecond;
                offset -= secondOfMinute / 60;
                secondOfMinute %= 60;
            }

            final int offsetMinute = offset % 60;
            offset /= 60;
            if (offsetMinute != 0) {
                minuteOfHour -= offsetMinute;
                offset -= minuteOfHour / 60;
                minuteOfHour %= 60;
            }

            final int offsetHour = offset % 24;
            offset /= 24;
            if (offsetHour != 0) {
                hourOfDay -= offsetHour;
                offset -= hourOfDay / 24;
                hourOfDay %= 24;
            }

            if (offset != 0) {
                dayOfMonth -= offset;
                if (dayOfMonth < 1) {
                    monthOfYear -= 1;
                    if (monthOfYear < 1) {
                        year -= 1;
                        monthOfYear = 12;
                    }
                    dayOfMonth = monthDays(year, monthOfYear);
                }
            }
        }

        try {
            return OffsetDateTime.of(year, monthOfYear, dayOfMonth,
                                     hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond,
                                     ZoneOffset.UTC);
        } catch (DateTimeException ex) {
            throw new TimestampParseException(ex);
        }
    }

    /**
     * Returns the number of days in the given month of the given year.
     *
     * The method is reimplemented based on month_days from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l199">month_days</a>
     */
    private static int monthDays(final int year, final int monthOfYear) {
        if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) {
            return leapYearMonthDays[monthOfYear - 1];
        } else {
            return commonYearMonthDays[monthOfYear - 1];
        }
    }

    private final String originalString;

    private final int dayOfMonth;
    private final int weekBasedYear;
    private final int hour;
    private final int dayOfYear;
    private final int nanoOfSecond;
    private final int minuteOfHour;
    private final int monthOfYear;
    private final Instant instantSeconds;
    private final int secondOfMinute;
    private final int weekOfYearStartingWithSunday;
    private final int weekOfYearStartingWithMonday;
    private final int dayOfWeekStartingWithMonday1;
    private final int weekOfWeekBasedYear;
    private final int dayOfWeekStartingWithSunday0;
    private final int year;

    private final String timeZoneName;

    private final String leftover;

    /**
     * Numbers of days per month and year.
     *
     * The constants are imported from LeapYearMonthDays and CommonYearMonthDays in Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l197">LeapYearMonthDays</a>
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l198">CommonYearMonthDays</a>
     */
    private static final int[] leapYearMonthDays = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    private static final int[] commonYearMonthDays = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
}
