Release 0.9.8
==================================

General Changes
----------------

* Bump up Guice to 4.0.0, and guice-bootstrap to 0.3.0 [#1008]
* Load dependencies of Maven-based plugins (only their "direct" dependencies) [#1012] [#1015]
* Find local files with path_prefix with case-sensitivity of PathMatcher of the runtime operating system [#1022] [#1040]

Bug Fixes
----------

* Fix the timestamp parser for a combination of epoch seconds and sub-seconds [#1033] [#1034]

Release Date
------------------
2018-10-09
