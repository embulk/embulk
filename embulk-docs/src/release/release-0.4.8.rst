Release 0.4.8
==================================

General Changes
------------------

* ``spi.time.TimestampParser`` supports microseconds.
* ``spi.time.TimestampFormatter`` supports milliseconds, microseconds and nanoseconds.
* Fixed a bug at ``spi.time.TimestampParser`` that returns wrong timestamp if format string is ``%s`` which means epoch seconds (@hiroyuki-sato++)
* ``embulk`` command installed by ``gem install embulk`` sets JVM optimization flags. This behavior is same with embulk.jar updated since 0.4.6.
* ``preview`` subcommand with ``-G, --vertical`` option uses CRLF on Windows

Release Date
------------------
2015-02-24
