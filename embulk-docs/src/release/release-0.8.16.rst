Release 0.8.16
==================================

General Changes
------------------

* Added remove_columns filter plugin [#530]

  * http://www.embulk.org/docs/built-in.html#remove_columns-filter-plugin

* Supported timestamp format "%Q". (@hiroyuki-sato) [#468, #531]

* Improved csv guess plugin:

  * Added semicolon as delimiter suggest candidate in csv guess plugin. [#527]

  * Enabled suggesting for a few rows [#533]

  * Enabled suggesting for a single column [#540]

* Changed and removed limitation of minimum 40 bytes size limit of guessing. [#518]

* Refactored and introduced TestingEmbulk#{Input,Parser,Output}Builder to embulk-test. [#513, #514, #526]

* Fixed PageBuilder to avoid NullPointerException. [#535]

* Fixed ResumableInputStream to avoid NullPointerException. [#472]

* Fixed TaskValidationException to inherit ConfigException. [#520]

* Fixed build.gradle to use Task.doLast instead of Task.leftShift. [#536]

* Fixed build failure on AppVeyor by FileNotFoundException. [#537]

* Added updateJRuby task to make it easy to upgrade version of JRuby. [#538]

* Upgraded gradle v3.2.1. [#528]

  * Release notes: https://docs.gradle.org/3.2.1/release-notes

Release Date
------------------
2017-01-27
