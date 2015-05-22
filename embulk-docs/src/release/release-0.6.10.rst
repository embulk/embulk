Release 0.6.10
==================================

General Changes
------------------

* PluginRegistry shows loaded plugin name and version to stdout
* Fixed a problem where ``embulk gem install`` command can't install gems which include a native extension
* Added ``embulk irb`` subcommand
* Fixed ``spi.ProcessTask`` serialization to preserve ``timestamp_format`` of a column. This fixes compatibility with embulk-executor-mapreduce.


Release Date
------------------
2015-05-21
