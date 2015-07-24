Release 0.6.19
==================================

Java Plugin API
------------------

* Added spi.unit.ToStringMap utility class which can be used as a field type of a Task class. ToStringMap is a ``Map<String,String>`` but also accepts non-string values and converts it to string. This fuzzy behavior is useful for options such as ``options``.


Embulk Embed API
------------------

* EmbulkService supports ``overrideModules`` method so that applications can use ``Modules.override`` to override specific injections.


Release Date
------------------
2015-07-23
