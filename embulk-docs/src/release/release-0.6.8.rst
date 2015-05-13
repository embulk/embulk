Release 0.6.8
==================================

Plugin API
------------------

* Added utility class ``spi.util.ResumableInputStream``
* Added utility class ``spi.util.RetryExecutor``

Built-in plugins
------------------

* ``parser-csv`` rejects rows if one includes too many columns by default. Setting ``allow extra_columns`` option to ``true`` will make the behavior same with before.
* ``guess-csv`` guesses ``columns`` option every time.

General Changes
------------------

* Fixed a problem that IntelliJ IDEA causes problem when it imports embulk source code.
* Fixed a problem that transaction silently succeeds when an exception happens after all taks succeeded.

Release Date
------------------
2015-05-12
