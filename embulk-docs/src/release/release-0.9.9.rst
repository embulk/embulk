Release 0.9.9
==================================

General Changes
----------------

* Add an experimental new EmbulkEmbulk.SimpleBootstrap, instead of EmbulkEmbed.Bootstrap [#1038] [#1048]
* Make selfrun scripts startable in Java 9, 10, 11 [#1020] [#1049] [#1056] [#1057]

Bug Fixes
----------

* Fix the timestamp parser so that `stop_on_invalid_record: false` works effective for invalid dates such as 2018-02-31 [#1052]
* Fix `embulk new` and `embulk migrate` to work on Windows: [#1031] [#1053] [#1054]

Release Date
------------------
2018-10-30
