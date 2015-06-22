Release 0.6.12
==================================

Plugin API
------------------

* Plugins can use both ``config[:key]`` (``task[:key]``) and ``config['key']`` (``task['key']``).
* Added ``Embulk.logger`` for Ruby plugins


Built-in plugins
------------------

* ``guess-csv`` plugin does not raise exceptions when a file has only 1 line
* ``parser-csv`` shows number of lines using 1-origin indexing rather than 0-origin


General Changes
------------------

* Guessing skips files smaller than 40 bytes so that guessing works when there is an empty file at the beginning.
* Uses ANSI color for log messages
* Uses logback instead of log4j for logging backend
* Added ``spi.Exec.isPreview()`` as an alias of ``spi.Exec.session().isPreview()``
* Plugin template for Ruby uses Xxx instead of XxxInputPlugin for the class name so that class name matches with file name.
* When parsing timestamp fails, exception message includes the original text (@yyamano++)


Release Date
------------------
2015-06-22
