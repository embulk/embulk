Release 0.7.9
==================================

General Changes
------------------

* Liquid template engine uses strict validation. When a liquid template file includes an error (e.g. included file does not exist), embulk command fails rather than including an error message in the config file (@notpeter++).
* Fixed liquid template engine processing.
* Fixed ``embulk bundle --help`` and ``embulk bundle help <command>`` to work.


Release Date
------------------
2015-11-11
