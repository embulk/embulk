Release 0.6.11
==================================

Built-in plugins
------------------

* ``input-file`` plugin sets ``last_path`` when there are no input files.
* ``parser-csv`` supports **comment_line_marker** option to skip lines starting with comment characters such as '#'.
* Fixed a bug where ``guess-csv`` guesses timestamp wrongly if order of date is month-day-year.

General Changes
------------------

* Command line execution supports ``-J``, ``-R``, ``-J+O``, and ``-J-O`` arguments on Windows (@hito4++)


Release Date
------------------
2015-05-30
