Release 0.9.0
==============


Java 8
-------

Embulk v0.9.0 is Java 8-based.

* Embulk v0.9 assumes Java 8, and drops Java 7-compatibility.
* All tests run on Java 8 in CI.
* ``org.embulk.spi.time.Timestamp`` uses ``java.time`` classes which are added in Java 8.


No more gem-based releases
---------------------------

`Gem packages of Embulk core <https://rubygems.org/gems/embulk>` are no longer released since v0.9.0. Embulk v0.8.39's Gem is the last Gem release.

* The structure of JAR packages has changed to run Bundler with Embulk v0.9.0. Embulk's Ruby code and its dependencies are contained in the JAR packages with Gem structures. The ``embulk bundle`` subcommand and the ``-b`` option do not need Gem packages of Embulk core installed.
* Users need to change their Gemfile for Bundler not to describe Embulk's explicit version. Users may have to uninstall older Gem packages of Embulk core.
* Ruby ``embulk/runner.rb`` has been removed.


Removals
---------

Some deprecated methods and classes have been eliminated in v0.9.0.

* ``org.embulk.spi.json.RubyValueApi``
* ``org.embulk.spi.time.Timestamp#getRubyTime`` and ``org.embulk.spi.time.Timestamp#fromRubyTime``
* Related to ``org.embulk.spi.time.TimestampFormatter``

  * ``org.embulk.spi.time.TimestampFormat#newFormatter``
  * ``org.embulk.spi.time.TimestampFormatter.FormatterTask``
  * ``org.embulk.spi.time.TimestampFormatter.Task#getJRuby``
  * ``org.embulk.spi.time.TimestampFormatter`` 's constructor ``TimestampFormatter(String, FormatterTask)``
  * ``org.embulk.spi.time.TimestampFormatter`` 's constructor ``TimestampFormatter(ScriptingContainer, String, org.joda.time.DateTimeZone)``

* Related to ``org.embulk.spi.time.TimestampParser``

  * ``org.embulk.spi.time.TimestampFormat#newParser``
  * ``org.embulk.spi.time.TimestampParser.ParserTask``
  * ``org.embulk.spi.time.TimestampParser.Task#getJRuby``
  * ``org.embulk.spi.time.TimestampParser`` 's constructor ``TimestampParser(String, ParserTask)``
  * ``org.embulk.spi.time.TimestampParser`` 's constructor ``TimestampParser(ScriptingContainer, String, org.joda.time.DateTimeZone)``
  * ``org.embulk.spi.time.TimestampParser`` 's constructor ``TimestampParser(ScriptingContainer, String, org.joda.time.DateTimeZone, String)``

* Related to ``org.embulk.spi.util.DynamicPageBuilder``

  * ``org.embulk.spi.util.DynamicPageBuilder.BuilderTask#getJRuby``
  * ``org.embulk.spi.util.DynamicPageBuilder`` 's constructor ``DynamicPageBuilder(BuilderTask, BufferAllocator, Schema, PageOutput)``
  * ``org.embulk.spi.util.dynamic.AbstractDynamicColumnSetter#setRubyObject``
  * ``org.embulk.spi.util.dynamic.SkipColumnSetter#setRubyObject``

* ``org.embulk.spi.util.PagePrinter`` 's constructor ``PagePrinter(Schema, TimestampFormatter.FormatterTask)``


Deprecation
------------

The following method has been newly deprecated since v0.9.0. They are to be removed by v0.10 (or possibly earlier).

* ``org.embulk.spi.ExecSession#newTimestampFormatter(String, org.joda.time.DateTimeZone)``


Packaging
----------

* ``embulk-jruby-strptime`` has been merged into ``embulk-core``.
* ``embulk-cli`` has been merged into ``embulk-core``.


Dependencies
-------------

* Embedded Bundler has been upgraded to 1.16.0.
* Embedded JRuby has been upgraded to 9.1.15.0.


General Changes
----------------

* The default Gem plugin directory has been moved to ``~/.embulk/lib/gem``.
* The default JAR plugin directory has been moved to ``~/.embulk/lib/m2/repository``.
* Guess plugins for standard plugins has been moved to ``embulk-standards``.
* New ``ruby:`` prefixed timestamp format is added.
* New ``java:`` prefixed timestamp format is added as experimental.
* Fix ``guess_sample_buffer_bytes`` option so that it enables ``GuessParserPlugin`` to read sample buffer customized by users.


Release Date
-------------
2018-01-30
