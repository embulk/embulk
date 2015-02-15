Release 0.4.0
==================================

Plugin Template Generator
------------------

A new CLI subcommand ``new`` generate a plugin template.

For example, ``new ruby-output <name>`` generates a template of output plugin written in Ruby::

    $ embulk new ruby-output foo
    Creating embulk-output-foo/
      Creating embulk-output-foo/README.md
      Creating embulk-output-foo/LICENSE.txt
      Creating embulk-output-foo/.gitignore
      Creating embulk-output-foo/Rakefile
      Creating embulk-output-foo/Gemfile
      Creating embulk-output-foo/embulk-output-foo.gemspec
      Creating embulk-output-foo/lib/embulk/output/foo.rb

The template generate supports most of plugin types. ruby-file-input and output, ruby-parser and formatter, ruby-decoder and encoder are not available yet because plugin SPI is not implemented yet.


Java Plugin Loader
------------------

Now, we can write plugins in Java.

The plugin loader creates a dedicated classloader for each plugin so that plugins can depend on different versions of libraries. To develop a Java plugin, you can use use ``new java-<category> <name>`` command which generates a template. The generated directory has ``build.gradle`` file so that you can run ``./gradlew gem`` command to build a gem package.


Plugin SPI
------------------

* **IMPORTANT**: Changed plugin path name from ``lib/embulk/<caetgory>_<name>.rb`` to ``lib/embulk/<category>/<name>.rb``.
* Added ``@ConfigInject`` annotation to set resources injected by Guice to a Task.
* Removed support for ``@JacksonInject`` annotation on a Task. ``@ConfigInject`` is the drop-in replacement.
* Changed class name ``spi.NextConfig`` to ``spi.ConfigDiff``.



Built-in plugins
------------------

* ``csv`` parser plugin and its guess plugin assume "true", "yes", "Y" and "on" as a boolean true value.

* ``file`` input plugin changed parameters.

  * Added ``path_prefix`` parameter (string, required).
  * Added ``last_path`` parameter (string, optional) which skips files older than or same with the specified file path.
  * Removed ``paths`` parameter.

* ``file`` output plugin changed parameters.

  * Added ``sequence_format`` parameter.
  * Removed ``directory`` parameters.
  * Removed ``file_name`` parameters.
  * The actual file name is ``${path_prefix}${sequence_format % [processorIndex, sequenceNumber}${ext_name}``.

* Removed ``s3`` file input plugin. It is now embulk-input-s3 gem.


General Changes
------------------

* PluginManager reports the cause of exceptions when it failed to load a plugin.
* Removed pom.xml.
* Set locale = 'en_US' when gradle builds javadoc.
* Added documentation scripts using Sphinx, YARD and JavaDoc.


Release Date
------------------
2015-02-15
