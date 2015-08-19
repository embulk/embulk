Release 0.7.1
==================================

General Changes
------------------

* Fixed ``no such file to load`` error when embulk is installed as a single jar file.

Java API
------------------

* Added exception class ``DataException`` and exception tag interface ``UserDataExceptoin``. If those exceptions are thrown, applications are suggested not to retry this bulk load because retried bulk import will fail again unless user's input data is fixed.

Ruby API
------------------

* ``ConfigError`` includes org.embulk.config.UserDataExceptoin interface.


Release Date
------------------
2015-08-18
