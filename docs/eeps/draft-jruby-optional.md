---
EEP: unnumbered
Title: JRuby as Optional
Author: dmikurube
Status: Draft
Type: Standards Track
Created: 2023-06-22
---

Introduction
=============

This EEP explains the decision to make JRuby just optional in Embulk. JRuby had been mandatory and played many roles historically in Embulk. On the other hand, JRuby had been the bottleneck and a blocker in Embulk.

Background: JRuby in Embulk
============================

JRuby had been weaved everywhere inside Embulk. As of Embulk v0.8.0, JRuby was used in the following portions, to name just a few major examples.

* The command-line parser
* All the plugin mechanisms, including the plugin registry, the loader, and the bootstrap of every plugin
* All the guess implementations (see "Case #1" in [EEP-3](./eep-0003.md))
* The timestamp parser and formatter (see "Case #4" in [EEP-3](./eep-0003.md))

It was definitely clear that Embulk did not work with JRuby. On the other hand, JRuby was the bottleneck of performance and development, and a blocker from evolving Embulk by being the source of unconscious dependencies.

Performance
------------

JRuby had been the performance bottleneck of Embulk, especially in the bootstrap. Embulk often got frozen for seconds with high CPU usage just to initialize JRuby.

The freeze happened even if the user used only Java-based plugins. Stopping using JRuby was the only option to get rid of the performance bottleneck.

JRuby consumed a non-negligible amount of memory, too.

Dependencies
-------------

JRuby has a lot of features and its own dependencies. Embulk and many Embulk plugins depended a lot directly and indirectly on JRuby and its dependencies. It had sometimes blocked maintainers from changing things in the Embulk core.

[EEP-3](./eep-0003.md) explains some of the dependency problems, including the JRuby upgrade.

Upgrading the embedded JRuby had been the representative challenge. Some features could be broken unexpectedly by upgrading JRuby. On the other hand, some Ruby-based plugins started to encounter a negative situation with the old JRuby, such as [`embulk-output-bigquery`](https://rubygems.org/gems/embulk-output-bigquery).

* [I can't use Google Cloud API through embulk v.0.10.2 (embulk/embulk#1251)](https://github.com/embulk/embulk/issues/1251)

Making JRuby Optional
======================

Embulk needed to be runnable without JRuby to mitigate the performance and dependency problems. JRuby needed to be just optional. This section explains how it is realized.

Making requisite changes
-------------------------

At first, finishing the following changes was mandatory to make Embulk runnable without JRuby.

### Command-line parser

Embulk's command-line parser had been implemented in Ruby. Therefore, JRuby had always been required.

The command-line parser has been re-implemented in pure-Java since v0.8.28.

* [Replace its command-line option parser in pure-Java. (embulk/embulk#543)](https://github.com/embulk/embulk/pull/543)

### Plugin mechanisms

JRuby is mandatory, obviously, when loading a RubyGems-style plugin. Another style of Embulk plugins was needed to make JRuby optional.

For that reason (and more), the new Maven-style plugins have been implemented. See [EEP-5](./eep-0005.md) for the details of Maven-style Plugins.

### Lazy loading

Even after the new pure-Java command-line parser and the Maven-style plugins were ready, JRuby was still initialized unconditionally in the bootstrap.

The JRuby initialization has been deferred until JRuby is really needed since v0.9.3.

* [Initialize JRuby's ScriptingContainer lazily (embulk/embulk#962)](https://github.com/embulk/embulk/pull/962)

Replacing plugins and plugin utilities
---------------------------------------

Even though the mandatory changes above were made, some utilities for plugins and some standard plugins still depended on JRuby. They needed to be changed next.

### Timestamps

Embulk's `TimestampParser` and `TimestampFormatter` depended on JRuby with a Ruby method `Date._strptime` and a Java class `org.jruby.util.RubyDateFormat`, respectively.

`TimestampParser` was re-implemented in Java in v0.8.27. `TimestampFormatter` also needed to be re-implemented without JRuby classes. `org.jruby.util.RubyDateFormat` had been deprecated even in JRuby.

* [Java timestamp parser and RubyDateParser v2 (embulk/embulk#611)](https://github.com/embulk/embulk/pull/611)
* [Avoid using org.jruby.util.RubyDateFormat in TimestampFormatter (embulk/embulk#830)](https://github.com/embulk/embulk/issues/830)

Finally, we implemented [`embulk-util-timestamp`](https://github.com/embulk/embulk-util-timestamp), a new standalone timestamp library compatible with Ruby's `strptime` and `strftime`, so that each plugin would use it by themselves. See "Case #4" in [EEP-3](./eep-0003.md) for some more information.

### Guess

All the standard guess procedures, including the schema guess, the timestamp format guess, and the CSV guess plugin, had been implemented in Ruby. In other words, JRuby was still mandatory to perform guesses.

We re-implemented all the guess procedures in pure-Java as a standalone library [`embulk-util-guess`](https://github.com/embulk/embulk-util-guess) so that each guess plugin would use it by itself. See "Case #1" in [EEP-3](./eep-0003.md) for more information.

Loading JRuby out of Embulk
----------------------------

JRuby has somehow gotten "optional" by the changes above. In the end, we determined to remove JRuby from Embulk's executable binary distribution. The decision was made for the following reasons.

* The JRuby-related classes had been loaded in the top-level class loader.
    * JRuby included some third-party libraries in it, for example, [Joda-Time](https://www.joda.org/joda-time/).
    * The loaded JRuby-related classes could potentially conflict with the plugin's dependencies. See [EEP-3](./eep-0003.md) about this.
    * JRuby needed to be loaded in a sub-class loader not to conflict with plugins.
* Users had to use the fixed version of JRuby embedded in Embulk.
    * Users encountered difficulties using some Ruby-based plugins, for example [`embulk-output-bigquery`](https://rubygems.org/gems/embulk-output-bigquery), as mentioned above.
    * On the other hand, upgrading the embedded JRuby could cause another difficulty. See also [EEP-3](./eep-0003.md) about this.
* Embulk's executable binaries had been so large, ~40MB.
    * JRuby took about half (~20MB) of this.

JRuby is no longer included in Embulk's executable distributions from Embulk v0.10.21. Now, a user needs to download the JRuby package by themselves, and to configure an Embulk System Property `jruby` with the downloaded JRuby package as below to use a JRuby-based feature, typically when using a RubyGems-style plugin. See [EEP-4](./eep-0004.md) for Embulk System Properties.

* [Use external JRuby optional (embulk/embulk#1336)](https://github.com/embulk/embulk/pull/1336)

```
# embulk.properties
jruby=file:///home/user/jruby-complete-9.1.15.0.jar
```

This change requires an additional step for users. At the same time, however, this change allows users to choose their preferred JRuby version by themselves.

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
