Release 0.7.2
==================================

Built-in plugins
------------------

* Added ``stop_on_invalid_record`` option to ``parser-csv`` plugin. This option stops bulkloading if it finds a broken record such as invalid timestamp or invalid integer format rathar than skipping it.


Ruby Plugin API
------------------

* Added ``Embulk::Exec.preview?`` which returns true if plugins are running in preview.
* Fixed ``cannot be cast to org.jruby.RubyException`` when ConfigException is raised.


General Changes
------------------

* Fixed ``embulk selfupdate`` subcommand (@cosmo0920++)


Release Date
------------------
2015-08-20
