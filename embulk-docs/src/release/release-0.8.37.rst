Release 0.8.37
==================================

General Changes
----------------

* Parse Ruby-style time zone names in addition [#833] [#840]
* Remove dead code around the Ruby timestamp parser replaced [#813] [#823]

Bug Fixes
----------

* Fix parsing leap seconds to get the same Timestamp with pure Ruby's [#842]
* Process yday (day of the year) in TimestampParser as well [#834] [#843]
* Fix nanoseconds set by StrptimeParser's %Q [#848]


Release Date
------------------
2017-11-21
