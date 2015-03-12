Release 0.4.9
==================================

CLI
------------------

* Fixed execution script for Windows

Built-in plugins
------------------

* Fixed a problem that ``encoder/gzip`` plugin didn't flush last buffer in memory.
* ``encoder/gzip`` plugin supports ``level`` option (@yaggytter++).

General Changes
------------------

* Fixed a problem of timestamp parser that ignores milliseconds if format is ``%s.%N``.
* Plugin template generator generates shorter description to fit in a table at the new plugin list page.

Release Date
------------------
2015-02-25
