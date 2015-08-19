Release 0.7.0
==================================

General Changes
------------------

* Upraded JRuby version to 9.0.0.0. Ruby scripting is compatible with Ruby 2.2 syntax.

* Added ``embulk migrate <plugin directory>`` subcommand. This command modifies plugin code to use the latest Embulk API.

* Enambed Liquid template engine. If configuration file name ends with ``.yml.liquid``, embulk embeds environment variables using Liquid template engine.

* Embulk gem package for JRuby doesn't include JRuby itself any more. Size of a gem package is reduced from 38MB to 7.6MB.

* Embulk gem is also released for CRuby. This enables us to install ``embulk`` command using ``gem install embulk``.

* **IMPORTANT**: ``embulk bundle`` command runs bundler. To create a new plugin bundle, use ``embulk bundle new <directory>`` command. To update gems, use ``embulk bundle`` command at the directory. Instructions are written at generated Gemfile file.


Ruby Plugin API
------------------

* Added experimental ``Embulk.setup`` and ``Embulk::Runner``.

  This enables ruby scripts to run embulk easily. This is also good for test code. For example, you can use this code:

.. code-block:: ruby

    require 'embulk'
    Embulk.setup
    Embulk::Runner.run(YAML.load_file("config.yml"))

* ``embulk new`` generates .ruby-version file with jruby-9.0.0.0 for ruby-based plugins.

  This makes plugin development easy as following:

.. code-block:: console

    # 1. Create plugin template
    $ embulk new ruby-parser awesome
    $ cd embulk-parser-awesome
    # or upgrade existent plugin: embulk migrate embulk-parser-awesome
   
    # 2. Install dependency gems including embulk itself at vendor/bundle directory
    $ bundle install --path vendor/bundle
   
    # 3. Create an example configuration file
    $ vi config.yml
   
    # 4. You can run embulk without building & installing gem
    $ bundle exec embulk run config.yml

* Constants defined at ``Embulk::Java`` are deprecated. They're still kept for backward compatibility but will be removed at a future release.

* Added ``Embulk::Java::Config`` and ``Embulk::SPI`` namespaces to access Java classes.


Java Plugin API
------------------

* **IMPORTANT**: Renamed CommitReport class to TaskReport. Binary backward compatibility is kept so that old plugins built with embulk 0.6.x can run with embulk 0.7.0. But this backward compatibility code will be removed at future release.

  To upgrade your plugin code, you can use ``embulk migrate <plugin directory>`` command.

* Upgraded gradle version to 2.6. This version supports ``./gradlew -t <task>`` command that watches changes of files and rebuild continuously.

  This makes plugin development easy as following:

.. code-block:: console

    # 1. Create plugin template
    $ embulk new java-input awesome
    $ cd embulk-input-awesome
    # or upgrade existent plugin: embulk migrate embulk-input-awesome
   
    # 2. Build code continously
    $ ./gradlew -t package
   
    # 3. Create an example configuration file
    $ vi config.yml
   
    # 4. Run embulk with -L option
    $ embulk -L . run config.yml

* Added ``EmbulkEmbed.Bootstrap`` class to build ``EmbulkEmbed`` instance.

* Added ``ConfigLoader.fromJsonString(String)`` and ``ConfigLoader.fromYamlString(String)`` methods.

* Added guess, preview, and run methods at ``EmbulkEmbed`` don't need ExecSession instance any more.

* EmbulkService is now deprecated. Replacement is EmbulkEmbed.


Release Date
------------------
2015-08-18
