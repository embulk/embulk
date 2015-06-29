Release 0.6.15
==================================

Plugin API
------------------

* PageBuilder sets null to a column if setXxx method is not called to the column.


Built-in plugins
------------------

* **IMPORTANT**: Because of default value of ``sequence_format`` option, ``formatter-csv`` adds ``.`` (dot) before ``file_ext`` rather than after ``path_prefix``. If you're using ``out: type: file``, see the document of file output plugin at :doc:`../built-in`.


General Changes
------------------

* ``preview`` reads next file (task) when the first file (task) is empty.
* Fixed serialize/deserialization bug of ColumnConfig.
* Fixed NullPointerException caused when ``PageBuilder#setRubyObject(IRubyObject)`` is called with ``nil``.


Release Date
------------------
2015-06-29
