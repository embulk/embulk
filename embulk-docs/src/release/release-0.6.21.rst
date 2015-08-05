Release 0.6.21
==================================

Built-in plugins
------------------

* Added ``filter-rename`` plugin. We can rename name of columns. This plugin has no impact on performance.
* ``parser-csv`` plugin accepts ``null`` to ``quote`` and ``escape`` options to disable quoting or escaping. This is useful if a file includes ``"`` in a non-quoted value.
* ``parser-csv`` shows warning if empty string is set to ``quote`` or ``escape`` options. Behavior is kept backward-compatible but it will be rejected in the future.


Java Plugin API
------------------

* Added ``config.DataSource.has`` method to check whether it contains a key or not.


Release Date
------------------
2015-08-05
