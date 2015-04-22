Release 0.6.4
==================================

Built-in plugins
------------------

* ``guess-csv`` keeps existent configurations such as ``null_string`` if they are already set.
* ``guess-csv`` skips line if a CSV file is broken rather than falling back to ad-hoc implementation which can't deal with quoted values.
* Fixed a problem where ``guess-csv`` can't handle quoted values (@y-ken++)

Release Date
------------------
2015-04-21
