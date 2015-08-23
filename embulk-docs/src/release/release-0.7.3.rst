Release 0.7.3
==================================

General Changes
------------------

* ``embulk selfupdate`` command gets optoinal ``<version>`` argument to upgrade or downgrade to specific version (@cosmo0920++).
* ``embulk migrate`` command assumes that source code file encoding is UTF-8. This fixes encoding exception on Windows environment.


Java Plugin API
------------------

* Fixed ``Embulk::Exec.preview?``.
* Fixed ``raise ConfigException``.
* added missing ``PluginLoadError`` class.


Release Date
------------------
2015-08-23
