Release 0.4.3
==================================

CLI
------------------

* All subcommands show current time with timezone and embulk's version number at the beginning.

Plugin API Changes
------------------

* ``Thread.currentThread().getContextClassLoader()`` no longer returns JRuby's classloader. It returns null so that dependent libraries fallback to appropriate ``this.getClass().getContextClassLoader()`` call.

Built-in plugins
------------------

* ``guess/csv`` guesses ``escape`` and ``null_string`` options.
* Fixed ``guess/csv`` fails if the csv file includes a timestamp value with timezone (@kinyuka++).
* Fixed memory leak at ``output/file`` (@akirakw++).
* Fixed ``input/file`` loads unnecessary files when it lists files from ``.``.

General Changes
------------------

* embulk-cli artifact is no longer released.
* embulk-standards artifact doesn't directly depend on dependencies of embulk-core.
* Updated the build script.

  * ``bintrayUpload`` task uploads embulk-<version>.jar.
  * ``release`` task actually releases gem to RubyGems and publishes jar files to Bintray.

Release Date
------------------
2015-02-17
