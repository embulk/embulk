Release 0.6.3
==================================

Plugin API
------------------

* Added ``org.embulk.spi.unit.ByteSize`` utility class. Plugins can use this class to allow units such as 'KB' or 'MB' in the configuration file.

Command-line
------------------

* Added '-L, --load PATH' argument to load a plugin form a local directory.

General Changes
------------------

* Updated template generator for Java so that generated build.gradle file has ``package`` task to create gemspec file.
* PreviewExecutor and GuessExecutor unwrap ExecutionException. Error stacktrace becomes simple.
* EmbulkService creates JRuby runtime and its execution environment for each Injector instances.

Release Date
------------------
2015-04-21
