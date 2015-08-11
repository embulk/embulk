Release 0.6.22
==================================

General Changes
------------------

* Added experimental support for Liquid template engine of configuration files. If configuration file name ends with ``.yml.liquid``, embulk runs embeds variables using Liquid template engine.

Ruby Plugin API
------------------

* Added ``Embulk.require_classpath`` method to initialize plugin test environment (@cosmo0920++)
* ``Embulk::ConfigError`` extends ``org.embulk.config.ConfigException``.
* ``Embulk::DataSource`` raises ``Embulk::ConfigError`` instead of StandardError.

Java Plugin API
------------------

* Added ``org.embulk.EmbulkEmbed`` to embed Embulk in an applications as a library.
* Added ``ConfigLoader.newConfigSource`` to create an empty ConfigSource.
* ``TempFileSpace.cleanup`` deletes files and directories recursively (@cosmo0920++)


Release Date
------------------
2015-08-11
