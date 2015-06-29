Release 0.6.14
==================================

Built-in plugins
------------------

* ``formatter-csv`` plugin supports ``default_timezone`` and ``column_options`` parameters so that we can set timestamp format for each columns individually.
* **IMPORTANT**: ``formatter-csv`` does not use the same timestamp format with input timestamp columns. If you're using ``formatter: type: csv`` with ``parser: type: csv-``, you need to set ``format`` option for each columns to keep using current behavior. See ``column_options`` option of CSV formatter plugin described at :doc:`../built-in`.
* ``guess-csv`` plugin keeps using ``delimiter`` option if already set rather than to overwrite it everytime.


Plugin API
------------------

* Added ``config.DataSource#remove(String)`` method.
* Added ``spi.ColumnConfig.getOption().``

  * ``spi.type.TimestampType.getFormat()`` is deprecated.
  * ``spi.ColumnConfig.getFormat()`` is deprecated.
  * ``spi.ProcessTask`` does not serialize ``TimestampType.format`` any more.

* Added utility methods for ``spi.Schema`` and ``spi.SchemaConfig``.

  * Added ``Schema.Builder`` class and ``Schema.builder()`` method.
  * Added ``Schema#lookupColumn(String)``.
  * Added utility methods to ``SchemaConfig``.

* Added ``spi.time.TimestampFormatter.Task`` and TimestampColumnOption

  * ``TimestampFormatter.FormatterTask`` is deprecated

* Added ``spi.time.TimestampParser.Task`` and TimestampColumnOption

  * ``TimestampParser.ParserTask`` is deprecated

* ``spi.time.TimestampFormat`` is deprecated


General Changes
------------------

* Fixed a problem where embulk shows input plugin name and version twice to log.


Release Date
------------------
2015-06-29
