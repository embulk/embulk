Release 0.8.9
==================================

General Changes
------------------

* Added ``date:`` option to timestamp parser. If format doesn't include date part, date option is used as the default date. For example, parsing text "02:46:29" with ``format: %H:%M:%S`` and ``date: 2016-02-03`` options returns "2016-02-03 02:46:29".

* Upgraded msgpack-core version from 0.8.6 to 0.8.7. Release notes of msgpack-java: https://github.com/msgpack/msgpack-java/blob/develop/RELEASE_NOTES.md


Release Date
------------------
2016-05-12
