Release 0.5.2
==================================

Built-in plugins
------------------

* ``parser-csv`` plugin supports ``skip_header_lines`` parameter to skip first some lines.

  * ``header_line`` parameter is obsoleted. Although the parameter still works for backward compatibility, setting both ``header_line`` and ``skip_header_lines`` becomes configuration error.

* ``guess-csv`` plugin guesses first ignorable lines and sets ``skip_header_lines`` parameter automatically.

* ``guess-csv`` plugin guesses quoted column names correctly.

* ``formatter-csv`` pugin supports ``delimiter`` parameter (@hiroyuki-sato++).

* ``output-stdout`` fixed warning messages due to double-release of buffers.


General Changes
------------------

* Improved error message when double-release of a ``spi.Buffer`` is detected.
* Fixed ``preview`` when a filter plugin changes schema (@llibra++).
* Fixed infinite loop at ``Embulk::FileOutput#flush`` (@goronao++). It happened if a formatter plugin written in Ruby writes more than 32KB of data.


Release Date
------------------
2015-03-11
