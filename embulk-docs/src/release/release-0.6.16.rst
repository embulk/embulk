Release 0.6.16
==================================

General Changes
------------------

* Class loader of plugins lookup classes from plugin's class loader first. This change solves the problem where a plugin uses newer (or older) version of jar library which conflicts with a jar library used by applications embedding embulk.
* Added ``ExecSession#cleanup()`` method. Applications embedding embulk as a library needs to call this method at the end of bulk load transaction.


Plugin API
------------------

* Added ``spi.util.TempFileSpace`` and ``spi.Exec.getTempFileSpace``. This utility allows plugins to create temporary files. Because the behavior of cleaning files up is not completely defined yet, it is recommended to test the behavior before deploying code using this API to production.
* Added ``spi.unit.LocalFile`` class. Plugins can use this class as a mapped value of configuration file when it needs a path to a local file. This class solves a problem that plugins don't work with distributed executor plugins (such as embulk-executor-mapreduce) if the plugins use ``String`` to get a local file path. Using this class, users can also embed contents of the file in the configuration file instead of setting path to a file.


Note
------------------

* This release includes changes around ClassLoader. Most of plugins should work without modification but it's still highly recommended to test your plugins localy before deploying this version to production systems.


Release Date
------------------
2015-07-01
