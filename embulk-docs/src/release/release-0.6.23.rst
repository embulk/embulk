Release 0.6.23
==================================

General Changes
------------------

* Fixed ``LoadError: no such file to load -- liquid`` which happens when embulk is loaded by bundler.

Ruby Plugin API
------------------

* ``Embulk::ConfigError`` extending ``org.embulk.config.ConfigException`` is reverted because of ``exception class/object expected`` error.


Release Date
------------------
2015-08-13
