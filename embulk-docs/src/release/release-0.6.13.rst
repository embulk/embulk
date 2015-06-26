Release 0.6.13
==================================

Plugin API
------------------

* Aded spi.util.DynamicPageBuilder utility class. This is a wrapper of PageBuilder which converts types automatically.
* ``page_builder`` used by Ruby plugins are now backed by DynamicPageBuilder. This means that Ruby plugins don't need type conversion code such as to_i or to_s any more.

Built-in plugins
------------------

* ``guess-csv`` plugin guesses ``trim_if_not_quoted`` option. This works well if a CSV file includes integers with extra spaces (such as CSV files exported by Excel).
* Fixed a problem where ``guess-csv`` plugin guessed type of a column as "string" if it includes non-empty null_string.

General Changes
------------------

* "-b" option installs bundler automatically if it is not installed rather shows "no such file to load -- bundler" error message.

Release Date
------------------
2015-06-25
