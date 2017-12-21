package org.embulk.spi.time;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RubyTimeParsed is TimeParsed from Ruby-style date/time formats.
 */
class RubyTimeParsed extends TimeParsed {
    private RubyTimeParsed(
            final String originalString,

            final int dayOfMonth,
            final int weekBasedYear,
            final int hour,
            final int dayOfYear,
            final long nanoOfSecond,
            final int minuteOfHour,
            final int monthOfYear,
            final long secondSinceEpoch,
            final long nanoOfSecondSinceEpoch,
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
        this.secondSinceEpoch = secondSinceEpoch;
        this.nanoOfSecondSinceEpoch = nanoOfSecondSinceEpoch;
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

    static class Builder extends TimeParsed.Builder {
        Builder(final String originalString) {
            this.originalString = originalString;

            this.century = Integer.MIN_VALUE;
            this.dayOfMonth = Integer.MIN_VALUE;
            this.weekBasedYear = Integer.MIN_VALUE;
            this.hour = Integer.MIN_VALUE;
            this.dayOfYear = Integer.MIN_VALUE;
            this.nanoOfSecond = Long.MIN_VALUE;
            this.minuteOfHour = Integer.MIN_VALUE;
            this.monthOfYear = Integer.MIN_VALUE;
            this.ampmOfDay = Integer.MIN_VALUE;
            this.secondSinceEpoch = Long.MIN_VALUE;
            this.nanoOfSecondSinceEpoch = Long.MIN_VALUE;
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

        @Override
        public TimeParsed build() {
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
                this.secondSinceEpoch,
                this.nanoOfSecondSinceEpoch,
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
        Builder setNanoOfSecond(final long nanoOfSecond) {
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
        Builder setSecondSinceEpoch(
                final long secondSinceEpoch, final long nanoOfSecondSinceEpoch) {
            this.secondSinceEpoch = secondSinceEpoch;
            this.nanoOfSecondSinceEpoch = nanoOfSecondSinceEpoch;
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
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

        private static final String[] DAY_OF_WEEK_ABBREVIATED_NAMES = new String[] {
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

        private static final String[] MONTH_OF_YEAR_FULL_NAMES = new String[] {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December" };

        private static final String[] MONTH_OF_YEAR_ABBREVIATED_NAMES = new String[] {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

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
        private long nanoOfSecond;
        private int minuteOfHour;
        private int monthOfYear;
        private int ampmOfDay;
        private long secondSinceEpoch;
        private long nanoOfSecondSinceEpoch;
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

    @Override
    public Timestamp toTimestamp(final int defaultYear,
                                 final int defaultMonthOfYear,
                                 final int defaultDayOfMonth,
                                 final org.joda.time.DateTimeZone defaultTimeZone) {
        final long secondSinceEpoch;
        final long nanoOfSecondSinceEpoch;

        if (this.secondSinceEpoch != Long.MIN_VALUE) {
            secondSinceEpoch = this.secondSinceEpoch;
            // Fractions by %Q are prioritized over fractions by %N.
            // irb(main):002:0> Time.strptime("123456789 12.345", "%Q %S.%N").nsec
            // => 789000000
            // irb(main):003:0> Time.strptime("12.345 123456789", "%S.%N %Q").nsec
            // => 789000000
            // irb(main):004:0> Time.strptime("12.345", "%S.%N").nsec
            // => 345000000
            nanoOfSecondSinceEpoch = this.nanoOfSecondSinceEpoch;
        } else {
            final int year = (this.year == Integer.MIN_VALUE ? defaultYear : this.year);

            // TODO: Calculate with java.time in Java 8.
            // set up with min this and then add to allow rolling over
            org.joda.time.DateTime datetime =
                new org.joda.time.DateTime(year, 1, 1, 0, 0, 0, 0, org.joda.time.DateTimeZone.UTC);
            if (this.dayOfYear != Integer.MIN_VALUE) {
                // yday is more prioritized than mon/mday in Ruby's strptime.
                datetime = datetime.plusDays(this.dayOfYear - 1);
            }
            else {
                if (this.monthOfYear != Integer.MIN_VALUE) {
                    datetime = datetime.plusMonths(this.monthOfYear - 1);
                } else {
                    datetime = datetime.plusMonths(defaultMonthOfYear - 1);
                }
                if (this.dayOfMonth != Integer.MIN_VALUE) {
                    datetime = datetime.plusDays(this.dayOfMonth - 1);
                } else {
                    datetime = datetime.plusDays(defaultDayOfMonth - 1);
                }
            }
            if (this.hour != Integer.MIN_VALUE) {
                datetime = datetime.plusHours(this.hour);
            }
            if (this.minuteOfHour != Integer.MIN_VALUE) {
                datetime = datetime.plusMinutes(this.minuteOfHour);
            }
            if (this.secondOfMinute != Integer.MIN_VALUE) {
                if (this.secondOfMinute == 60) {
                    // Leap seconds are considered as 59 when Ruby converts them to epochs.
                    datetime = datetime.plusSeconds(59);
                }
                else {
                    datetime = datetime.plusSeconds(this.secondOfMinute);
                }
            }
            secondSinceEpoch = datetime.getMillis() / 1000;
            nanoOfSecondSinceEpoch = this.getNanoOfSecond(0);
        }

        final String zone = this.timeZoneName;
        final String text = this.originalString;

        // TODO: Calculate with java.time in Java 8.
        final org.joda.time.DateTimeZone timeZone;
        if (zone != null) {
            // TODO: Cache parsed zone?
            // TODO: Use Ruby's timezone instead of Joda-Time's?  https://github.com/embulk/embulk/issues/833
            timeZone = TimestampFormat.parseDateTimeZone(zone);
            if (timeZone == null) {
                throw new TimestampParseException("Invalid time zone name '" + zone + "' in '" + text + "'");
            }
        } else {
            timeZone = defaultTimeZone;
        }

        final long secondSinceEpochInUtc = timeZone.convertLocalToUTC(secondSinceEpoch * 1000, false) / 1000;
        return Timestamp.ofEpochSecond(secondSinceEpochInUtc, nanoOfSecondSinceEpoch);
    }

    @Override
    public Map<String, Object> asMapLikeRubyHash() {
        final HashMap<String, Object> hash = new HashMap<>();

        putIntIfValid(hash, "mday", this.dayOfMonth);
        putIntIfValid(hash, "cwyear", this.weekBasedYear);
        putIntIfValid(hash, "hour", this.hour);
        putIntIfValid(hash, "yday", this.dayOfYear);
        putFractionIfValid(hash, "sec_fraction", this.getNanoOfSecond());
        putIntIfValid(hash, "min", this.minuteOfHour);
        putIntIfValid(hash, "mon", this.monthOfYear);
        putSecondWithFractionIfValid(hash, "seconds",
                                     this.secondSinceEpoch,
                                     this.getNanoOfSecondSinceEpoch());
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

    private BigDecimal putFractionIfValid(final Map<String, Object> hash, final String key, final long value) {
        if (value != Long.MIN_VALUE) {
            return (BigDecimal) hash.put(key, BigDecimal.ZERO.add(BigDecimal.valueOf(value, 9)));
        }
        return null;
    }

    private Object putSecondWithFractionIfValid(
            final Map<String, Object> hash, final String key, final long second, final long fraction) {
        if (second != Long.MIN_VALUE) {
            if (fraction == Long.MIN_VALUE) {
                return hash.put(key, second);
            } else {
                return hash.put(key, BigDecimal.valueOf(second).add(BigDecimal.valueOf(fraction, 9)));
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

    private long getNanoOfSecond() {
        return this.nanoOfSecond;
    }

    private long getNanoOfSecond(final long defaultValue) {
        if (this.nanoOfSecond == Long.MIN_VALUE) {
            return defaultValue;
        }
        return this.nanoOfSecond;
    }

    private long getNanoOfSecondSinceEpoch() {
        if (this.nanoOfSecondSinceEpoch == Long.MIN_VALUE) {
            return 0;
        }
        return this.nanoOfSecondSinceEpoch;
    }

    private final String originalString;

    private final int dayOfMonth;
    private final int weekBasedYear;
    private final int hour;
    private final int dayOfYear;
    private final long nanoOfSecond;
    private final int minuteOfHour;
    private final int monthOfYear;
    private final long secondSinceEpoch;
    private final long nanoOfSecondSinceEpoch;
    private final int secondOfMinute;
    private final int weekOfYearStartingWithSunday;
    private final int weekOfYearStartingWithMonday;
    private final int dayOfWeekStartingWithMonday1;
    private final int weekOfWeekBasedYear;
    private final int dayOfWeekStartingWithSunday0;
    private final int year;

    private final String timeZoneName;

    private final String leftover;
}
