Release 0.6.26
==================================

Built-in plugins
------------------

* Added ``stop_on_invalid_record`` option to ``parser-csv`` plugin. This option stops bulkloading if it finds a broken record such as invalid timestamp or invalid integer format rathar than skipping it.


Ruby Plugin API
------------------

* Added ``Embulk::Exec.preview?`` which returns true if plugins are running in preview.

Release Date
------------------
2015-08-20
