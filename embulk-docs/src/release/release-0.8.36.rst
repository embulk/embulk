Release 0.8.36
==================================

General Changes
----------------

* Load dependency JAR files embedded in plugin JAR [#792]
* Improve timestamp parsing in Ruby plugins [#812] [#814]
* Notify Embulk-announce mailing list in CLI [#816]

Bug Fixes
----------

* Use single-quotes to quote path strings in YAML for Windows [#805]
* Truncate output file before overwriting it [#807]
* Fix typo in FALSE_STRINGS and add their test [#810]

Built-in plugins
-----------------

* Add new option ACCEPT_STRAY_QUOTES_ASSUMING_NO_DELIMITERS_IN_FIELDS in CSV parser [#809]

Deprecations
-------------

* Deprecate JRuby-dependent classes and methods [#800] [#803] [#825]
* Warn explicitly for deprecated methods [#821] [#826]


Release Date
------------------
2017-10-24
