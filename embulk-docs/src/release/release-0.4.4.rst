Release 0.4.4
==================================

CLI
------------------

* Supports ``-l, --log-level LEVEL`` option to specify log level. (@ykubota++)

Plugin API Changes
------------------

* Added ``Exec.isPreview()`` method which returns true if the transaction is running in in preview mode.
* Added ``util.OutputStreamFileOutput`` class.
* ``util.FileOutputOutputStream`` requires ``CloseMode`` at the constructor.
* When ``Embulk::DataSource#prop`` returns a String, Array, or Hash value, it returns a copy of the value.
* When ``Embulk::DataSource#prop`` returns a Hash value, it returns ``DataSource`` instance.

Plugin SPI
------------------

* Changed local variable name ``processorIndex`` to ``taskIndex``.
* Changed local variable name ``processorCount`` to ``taskCount``.

Built-in plugins
------------------

* Added ``gzip`` encoder plugin.

General Changes
------------------

* ``preview`` runs filter plugins in addition to input plugins.
* Fixed a problem of ``guess`` where guess plugins ignore formatter properties (such as ``charset``) guessed by other guess plugins (@hiroyuki-sato++).
* Log messages include "transaction", "resume", "cleanup", or task index at the header.
* Timestamp in log messages include time zone.

Release Date
------------------
2015-02-18
