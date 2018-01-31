Timestamp Parsing
==================

Embulk's `TIMESTAMP` type consists of POSIX time and its fractional part in nanoseconds **without any time zone information**. This constitution makes parsing temporal strings a bit complicated.

Basic design policy to parse timestamps is described in this writing.


Background
-----------

Parsing temporal strings is not simple, mainly due to time zones. There are two types of time zones:

1. Fixed time offsets (UTC offsets) which are fully-resolved fixed offsets from UTC.
2. Region-based time zones which represent geographical regions where specific rules of possible offset transitions (e.g. summer time) apply. The ["tz database"](https://en.wikipedia.org/wiki/Tz_database) is usually used to describe these areas.

Parsing a temporal string with (2) region-based time zones has problems.


### Time offset transitions

Region-based time zones may have time offset transitions such as summer time. Such transitions make parsing non-deterministic.

Assuming the default time zone is set to `"America/Los_Angeles"` for example, consider `"2017-03-12 02:34:56"` is given. That time did not exist in `"America/Los_Angeles"`, actually! At the moment of 02:00:00, clocks were turned forward an hour to 03:00:00 on March 12, 2017 to start the Daylight Saving Time. See [Clock Changes in Los Angeles, California, USA in 2017](https://www.timeanddate.com/time/change/usa/los-angeles?year=2017).

A similar problem happens when `"2017-11-05 01:23:45"` is given. That time came twice in `"America/Los_Angeles"`. At the moment of 02:00:00 (in Summer Time), clocks were turned backward an hour to 01:00:00 (in Winter Time).

Time offset transitions can happen not only for summer time, but for government/political decisions. For example, the Independent State of Samoa determined to change their time zone from UTC-11:00 (in Winter Time) to UTC+13:00 (in Winter Time) as of the end of 2011. At the end of December 29, 2011 (in UTC-11:00), Samoa continued directly to December 31, 2011. They skipped the entire calendar day of December 30, 2011 (in UTC+13:00). It means that `"2011-12-30"` is totally ineffective in `"Pacific/Apia"`. See [Time in Samoa at Wikipedia](https://en.wikipedia.org/wiki/Time_in_Samoa).


### Changes in the transition rules themselves

Updates in the tz database are not exceptional. The tz database is usually updated several times per year, and the updates are sometimes applied at the last minute.

For example, the above change of Samoa was applied in the tz database just 4 months before the change. See [the mailing list](http://mm.icann.org/pipermail/tz/2011-August/008703.html) and [GitHub](https://github.com/eggert/tz/commit/94b2c39254c2adb82a5fc6a68e507e546836a018).

Past transitions are sometimes modified as well. For example, summer time information of 1948-1951 in Japan was corrected in 2018. See [the mailing list](http://mm.icann.org/pipermail/tz/2018-January/025896.html) and [GitHub](https://github.com/eggert/tz/commit/bbd0ea690201acab766db57142f9aa0abba30613).

Such changes in the transition rules make parsing timestamps more complicated. For example, `"2011-12-31 12:34:56 Pacific/Apia"` can be parsed into different results by environments because the tz database used in Java is coupled with Java VM installation. The time is parsed into `"2011-12-31 12:34:56 +13:00"` by Java VM with the latest tz database while it is parsed into `"2011-12-31 12:34:56 -11:00"` with a bit stale tz database.


Region-based time zones in the core framework
----------------------------------------------

Handling exceptional cases like the above in common is a hard decision. A bunch of configurations may be wanted eventually such as:

* Some users may want errors for "impossible" times (e.g. `"2017-03-12 02:34:56 America/Los_Angeles"`)
* Some users may want to aggregate impossible times in summer time transitions to the earlier or the later.
* Some users may want ...

It is unrealistic to extend the timestamp parser in the core framework (and its configurations) to support all wants eventually. Such customizability should be provided in a separate plugin, not in the core framework. A typical approach would be a filter plugin to convert temporal `STRING` with region-based time zones into `TIMESTAMP` or still `STRING` with fixed time offsets.


Parsing until v0.8
-------------------

`TimestampParser` until Embulk v0.8 has accepted region-based time zones which can cause the problems above. Not only that, its time zone handling has had some bugs that `"2017-07-01 12:34:56 PDT"` is parsed as `"2017-07-01 12:34:56 -08:00"`, not `"-07:00"`. See the [Issue #860](https://github.com/embulk/embulk/issues/860) for details.

Embulk has parsed timestamps like this for a long time. It is a difficult decision to change the behavior since v0.9.


New-style parsing since v0.9
-----------------------------

Instead of modifying the exsiting timestamp parser, Embulk v0.9 has additional new-style parsers.

* Existing `TimestampParser` configurations like `{ format: "%Y-%m-%d %H:%M:%S" }` keep working for a while at least during Embulk v0.9.
* New-style configurations prefixed with `ruby:` is added in Embulk v0.9. `{ format: "ruby:%Y-%m-%d %H:%M:%S" }` works similarly to the existing configurations while it accepts only the time zones below which are compatible with Ruby's `Time.strptime`:
    * Fixed time offsets starting with `'-'` or `'+'`
    * `"Z"`, `"UTC"`, `"UT"`, and `"GMT"`
    * `"EST"`, `"EDT"`, `"CST"`, `"CDT"`, `"MST"`, `"MDT"`, `"PST"`, and `"PDT"` (considered as fixed offsets)
    * Military time zones `"A"` to `"Y"`
* New-style configurations prefixed with `java:` is **experimentally** added in Embulk v0.9. `{ format: "java:uuuu-MM-dd HH:mm:ss" }` works as `java.time.format.DateTimeFormatter` ([JSR 310: Date and Time API](https://jcp.org/en/jsr/detail?id=310)) while it intentionally rejects region-based time zones.


### `TimestampFormatter`

Configurations of `TimestampFormatter` basically follow `TimestampParser`.


Deprecation
------------

The existing `TimestampParser` without any format prefix may get deprecated (not removed) at some point during v0.9. It is totally TBD when the deprecated parser is finally **removed**, but it's kept at least during v0.9.


`TimestampParser` as a library
-------------------------------

`TimestampParser` (and `TimestampFormatter`) **may** be extracted as a separate Java library from the core framework in the future (TBD). They are not essential parts of the core framework, but just utilities for conversion. Keeping them in the core framework cause other dependency and compatibility problems.
