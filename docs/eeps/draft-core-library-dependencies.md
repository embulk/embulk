---
EEP: unnumbered
Title: Core Library Dependencies
Author: dmikurube
Status: Draft
Type: Standards Track
Created: 2023-12-22
---

Problem: dependency conflicts
==============================

The Embulk core has several library dependencies of its own because it has to do a certain amount of work itself. For example, [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) for parsing the user's Embulk configuration YAML, [Jackson](https://github.com/FasterXML/jackson) for processing user configurations, [Guice](https://github.com/google/guice) for managing its own object lifecycles, [Guava](https://github.com/google/guava) for utilities, [SLF4J](https://www.slf4j.org/) and [Logback](https://logback.qos.ch/) for logging, ...

On the other hand, almost all Embulk plugins need their own library dependencies for their own purposes. This includes transitive dependencies. For example, some AWS-related plugins need the [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html), [whose v1 transitively needed Jackson](https://central.sonatype.dev/artifact/com.amazonaws/aws-java-sdk-core/1.12.385/dependencies). Some Google Cloud-related plugins require the [Google Cloud Client Library for Java](https://github.com/googleapis/google-cloud-java), [which transitively requires Jackson and Guava](https://central.sonatype.dev/artifact/com.google.cloud/google-cloud-storage/2.17.1/dependencies).

The Embulk core assumes that it will be loaded by the top-level class loader (a.k.a. [system class loader or application class loader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassLoader.html#builtinLoaders)). An Embulk plugin is loaded by another "child" class loader that has the top-level class loader as its parent.

The dependencies of an Embulk plugin could have conflicted with the dependencies of the Embulk core. This caused a dependency "deadlock" until Embulk v0.9. Upgrading a dependency of the Embulk core could break some Embulk plugins. Upgrading a dependency library of some Embulk plugins was not possible without upgrading a dependency of the Embulk core. Upgrading all the Embulk plugins in the world was uneasy because all the Embulk plugins are not maintained by the Embulk project, but by volunteer developers in the world. Even if such an upgrade were achieved once, another upgrade would come again and again.

This was the main purpose of the "development" v0.10 series. It was the main reason why a lot of incompatibilities were introduced between Embulk v0.9 and v0.11 through Embulk v0.10.0 - v0.10.50.

This EEP provides an overview of the changes made to the dependencies of the Embulk core through v0.10.0 - v0.10.50, and explains why the changes were necessary.

Approaches: against the conflicts
==================================

The dependency situations differ depending on the use-case of each dependency library. To deal with the dependency problems, we have adopted four types of approaches depending on the characteristics of each dependency.

1. Hide
2. Remove
3. Keep
4. Keep, but remove or hide in the future

Many of the dependency libraries of the Embulk core are still used in Embulk v0.11, but hidden (#1) from plugins. The "hidden" dependency libraries of the Embulk core are loaded by the other child class loader. In Java's class loading mechanism, a class loaded by a child class loader can find a class loaded by its parent class loader, but cannot find a class loaded by its "sibling" class loader.

An Embulk plugin cannot find the classes of the hidden dependency libraries, so they would not conflict. These hidden dependency libraries are bundled as `embulk-deps`, another artifact of the Embulk core. The class loading mechanisms for hiding dependencies behind `embulk-deps` will be explained in another upcoming EEP.

Some of the dependency libraries are simply removed (#2) if their use was not very critical, or if it was unfortunately difficult to keep them in `embulk-deps`.

Embulk plugins up to Embulk v0.9 expected the dependency libraries of the Embulk core to be loaded in the parent class loader. Hiding (#1) or removing (#2) them causes critical incompatibilities. However, the dependency deadlock problem could not be solved as long as these dependency libraries were visible to Embulk plugins. We concluded that the incompatibility was unavoidable to solve the dependency deadlock, and decided to "go with it" through the Embulk v0.10 "development" series.

One of the dependency libraries (SLF4J) is kept (#3) up until now and from here on out.

Two of the dependency libraries (MessagePack for Java and Logback) will be kept for a while, but are planned to be hidden or removed (#4).

Walkthrough: each library
==========================

Let's walk through each library to analyze, discuss, and explain the design decisions for each case.

Jackson
--------

Jackson caused the most serious dependency conflicts. Let's start with Jackson.

[Jackson](https://github.com/FasterXML/jackson) is the de-facto standard JSON library for Java. It consists of three core modules and a bunch of add-on modules. These are separate Maven artifacts with transitive dependencies. For example:

* [`jackson-databind`](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind) depends on [`jackson-core`](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-core) and [`jackson-annotations`](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-annotations).
* [`jackson-datatype-jdk8`](https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jdk8) depends on `jackson-databind` and `jackson-core`.
* [`jackson-module-guice`](https://central.sonatype.com/artifact/com.fasterxml.jackson.module/jackson-module-guice) also depends on `jackson-databind`, `jackson-core`, and `jackson-annotations` along with [Guice](https://github.com/google/guice) and [Guava](https://github.com/google/guava).

Jackson does not expect different versions of its modules to be combined. For example, `jackson-datatype-jdk8:2.15.3` expects to be loaded with `jackson-core:2.15.3`. No one can tell what will happen if `jackson-datatype-jdk8:2.15.3` is loaded with `jackson-core:2.6.7`.

The Embulk core used to use `jackson-core`, `jackson-annotations`, `jackson-databind`, `jackson-datatype-jdk8`, and some more add-on modules. Their versions were `2.6.7`.

What if an Embulk plugin needs to use `jackson-dataformat-cbor:2.13.5`? The Embulk core has already loaded `jackson-core:2.6.7`, `jackson-annotations:2.6.7`, `jackson-databind:2.6.7`, and `jackson-datatype-jdk8:2.6.7` in its top-level class loader, which cannot be overridden. The Embulk plugin still needs `jackson-dataformat-cbor:2.13.5`. They... do not match.

On the other hand, what if the Embulk core upgrades its own Jackson dependencies to `2.15.3`? Some Embulk plugins in the world may use `jackson-dataformat-cbor:2.6.7` to be coupled with the existing Embulk core. This plugin may break when the Embulk core upgrades its own Jackson dependencies to `2.15.3`. They do not match either.

This is the "deadlock". The deadlock could be solved by upgrading all dependencies of the Embulk core and plugins at once, but all plugins are not maintained by the Embulk project, but by volunteer developers in the world. Upgrades would be delayed and misaligned. Furthermore, even if such a simultaneous upgrade were achieved, the next upgrades would come soon, again and again. Repeating such upgrades would not work in the community.

Finally, we decided to make Jackson in the Embulk core somehow invisible to plugins, so that the Embulk core and plugins could upgrade their own dependencies by their own decisions at their own times, without any deadlock. Of course, this would introduce critical incompatibilities, but we would have to accept them. It couldn't be helped.

Jackson was still needed to process user configurations in the Embulk core. Jackson cannot simply be removed. Then the Jackson libraries are "hidden" (#1) in `embulk-deps` since [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32).

Almost all Embulk plugins have to process user configurations. Many Embulk plugins use Jackson also for other purposes. [Embulk plugins are asked to have their own Jackson dependencies in order to work with Embulk v0.10.](https://dev.embulk.org/topics/get-ready-for-v0.11-and-v1.0-updated.html) User configurations are expected to be processed in plugins by [`embulk-util-config`](https://github.com/embulk/embulk-util-config), which uses Jackson in it.

The Embulk core would become more compact, and plugins would take on more responsibility. This is also part of [the "Compact Core" Principle in EEP-3](./eep-0003.md).

SnakeYAML
----------

[SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) is a YAML processor used by Embulk to parse user configuration YAML.

SnakeYAML is only used to parse the user configuration YAML. Once SnakeYAML parses the user configuration YAML, the user configurations are converted to Jackson objects. Almost all Embulk plugins are not expected to use SnakeYAML itself.

SnakeYAML has been "hidden" (#1) in `embulk-deps` since [Embulk v0.9.23](https://github.com/embulk/embulk/releases/tag/v0.9.23) with no major impact expected.

Java Bean Validation API and Apache BVal
-----------------------------------------

The [Java Bean Validation](https://jcp.org/en/jsr/detail?id=303) was used with [Apache BVal](https://bval.apache.org/) to validate user configurations, such as "must not be empty", "must be a positive integer", and so on.

However, in Embulk v0.10 and later, user configurations are processed in plugins with `embulk-util-config` as explained above. The Embulk core no longer needs to include the Bean Validation API and Apache BVal.

In addition, the Bean Validation API was affected by the [transition from Java EE to Jakarta EE](https://blogs.oracle.com/javamagazine/post/transition-from-java-ee-to-jakarta-ee). Even if the Embulk core provided the Bean Validation API (`javax.validation.*`) for plugins, the `javax.validation.*` packages were no longer maintained. It would have to be migrated to the new Jakarta Bean Validation API (`jakarta.validation.*`) at some point. Not only would this be another breaking change, but `jakarta.validation.*` is not in the official Java specification. It may have another breaking change in the future.

Furthermore, the Bean Validation API and Apache BVal require [Java Architecture for XML Binding (JAXB), which has been removed from Java SE since Java 11](https://openjdk.org/jeps/320). The Embulk core would have to include JAXB dependencies to keep the Bean Validation API and Apache BVal while migrating JAXB from `javax.xml.*` to `jakarta.xml.*`. They are too complicated to keep.

The Bean Validation API and Apache BVal were once hidden behind `embulk-deps` in [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32), and then simply removed (#2) from the Embulk core in [Embulk v0.10.49](https://github.com/embulk/embulk/releases/tag/v0.10.49).

Guice
------

Dependency injection by [Guice](https://github.com/google/guice) had laid down its roots deep in the core architecture of Embulk. However, the actual use of Guice in Embulk was not the real dependency injection, but so-called [the service locator anti-pettern](https://blog.ploeh.dk/2010/02/03/ServiceLocatorisanAnti-Pattern/). The [`Injector`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Injector.html) instance was dragged around and stored everywhere. The [`injector.getInstance(....class)`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Injector.html#getInstance-java.lang.Class-) calls were spreading everywhere, not only when constructing instances, but as needed at any runtime. [The control was not actually inverted](https://en.wikipedia.org/wiki/Inversion_of_control), but the dependencies were randomly woven throughout the entire Embulk architecture.

Should this have been fixed to "real dependency injection" with Guice? Actually, no. The bottom line is that Embulk is basically designed for one-shot execution in one Java process. Lifecycle management is not required. Embulk does not have a strong need for dependency injection containers. (JFYI, users could tweak `EmbulkEmbed` for multiple executions in one Java process, but this is not recommended. It could easily cause other unavoidable problems like class loader leaks.)

Also, Guice drags Guava (explained below), which is often a source of unintended incompatibility.

Hiding Guice behind `embulk-deps` is not trivial. And having Guice in the top-level class loader also includes Guava in the top-level class loader. Guice has been removed (#2) from the Embulk core since [Embulk v0.10.33](https://github.com/embulk/embulk/releases/tag/v0.10.33).

Guava
------

[Guava](https://github.com/google/guava) is a famous standard-ish Java library that includes utility classes such as immutable collections. On the other hand, Guava is known for often introducing breaking changes between versions.

* [Compatibility (google/guava)](https://github.com/google/guava/wiki/Compatibility)
* [Bug 427862 - Incompatible Guava versions/dependencies used by projects (eclipse.org)](https://bugs.eclipse.org/bugs/show_bug.cgi?id=427862)

The deadlock problems similar to Jackson (explained above) would occur. The Embulk core had Guava 18.0. This Guava 18.0 was on the top-level class loader, which could not be overridden from an Embulk plugin. An Embulk plugin couldn't use a later version of Guava. Upgrading Guava in the Embulk core could break some Embulk plugins due to Guava incompatibility. This is also the deadlock.

Like Jackson's case, we had to make Guava invisible to Embulk plugins. Unlike Jackson's case, Guava was not really mandatory once Guice was gone. Almost all of Guava's use-cases could be easily replaced with Java 8 standards. Guava is no longer wanted.

Guava has just been removed (#2) since [Embulk v0.10.39](https://github.com/embulk/embulk/releases/tag/v0.10.39).

JRuby
------

[JRuby](https://www.jruby.org/) was mixed in the top-level class loader of the Embulk core. Not only JRuby itself, JRuby contained a lot of its own dependency libraries like Joda-Time (explained below).

As explained in [EEP-6: JRuby as Optional](./eep-0006.md), JRuby has been optional under another child class loader since [Embulk v0.10.21](https://github.com/embulk/embulk/releases/tag/v0.10.21).

Joda-Time
----------

[Joda-Time](https://www.joda.org/joda-time/) was used in the Embulk core to represent and calculate timestamps (date and time), but Joda-Time is no longer actively maintained. Users of Joda-Time are asked to migrate to JSR-310 (`java.time`), which is included in the Java 8 standards.

Joda-Time has been deprecated in Embulk along with making JRuby optional (explained above), and has been completely removed (#2) since [Embulk v0.10.30](https://github.com/embulk/embulk/releases/tag/v0.10.30).

ICU4J
------

[ICU4J](https://unicode-org.github.io/icu/userguide/icu4j/) (The International Components for Unicode for Java) was used in the Embulk core only for character set detection in Guess plugins.

ICU4J was unlikely to cause dependency conflicts between the Embulk core and plugins, since many plugins did not depend on ICU4J.

While on the other hand, ICU4J is so huge. For example, `icu4j-54.1.1.jar` is ~10.6MB, `icu4j-57.2.jar` is ~10.8MB, and `icu4j-74.2.jar` is ~13.6MB. ICU4J occipied a quarter to a half in Embulk packages. We doubted that it was really worth having the entire ICU4J dependency in the Embulk core, since ICU4J was only used for character set detection.

Finally, ICU4J has been removed (#2) from the Embulk core since [Embulk v0.10.45](https://github.com/embulk/embulk/releases/tag/v0.10.45). Instead, we have copied some classes for character set detection from ICU4J 57.2 into [`embulk-util-guess`](https://github.com/embulk/embulk-util-guess). They are licensed under the ICU License, which is considered compatible with the X license, hence the MIT License. Guess plugins that need to detect character sets would use their own character set detection in `embulk-util-guess`.

Apache Maven
-------------

[Apache Maven](https://maven.apache.org/) has been used in the Embulk core for [EEP-5: Maven-style Plugins](./eep-0005.md) since [Embulk v0.9.8](https://github.com/embulk/embulk/releases/tag/v0.9.8). It has been hidden (#1) behind a child class loader since [Embulk v0.9.15](https://github.com/embulk/embulk/releases/tag/v0.9.15).

Apache Velocity
----------------

[Apache Velocity](https://velocity.apache.org/) is a template engine used in the Embulk core to "scaffold" plugin source code via the `embulk new` subcommand. It was hidden behind a child class loader since [Embulk v0.9.17](https://github.com/embulk/embulk/releases/tag/v0.9.17).

Finally, it was removed (#2) along with the removal of the `embulk new` subcommand in [Embulk v0.10.37](https://github.com/embulk/embulk/releases/tag/v0.10.37).

Apache Commons CLI
-------------------

[Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) is a command-line option parser used in the Embulk core since [Embulk v0.8.28](https://github.com/embulk/embulk/releases/tag/v0.8.28).

It has been hidden (#1) behind a child class loader since [Embulk v0.9.17](https://github.com/embulk/embulk/releases/tag/v0.9.17).

Apache Commons Lang 3
----------------------

[Apache Commons Lang](https://commons.apache.org/proper/commons-lang/) 3 has been used in the Embulk core for transitive dependencies from other Apache libraries such as Apache BVal, Apache Maven, and Apache Commons CLI.

Apache Commons Lang 3 was finally hidden (#1) behind `embulk-deps` in [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32) along with hiding Apache BVal. It is still there because of Apache Maven and Apache Commons CLI although Apache BVal was removed as of [Embulk v0.10.49](https://github.com/embulk/embulk/releases/tag/v0.10.49).

Airlift's Slice
----------------

[Airlift's Slice](https://github.com/airlift/slice) library was used from the very beginning of Embulk to manipulate Java byte arrays for packing/unpacking Embulk column values in `org.embulk.spi.Page`.

Airlift's Slice was unlikely to cause dependency conflicts between the Embulk core and plugins, since almost all plugins did not depend on Slice.

While on the other hand, Airlift's Slice has started to depend on [OpenJDK's Java Object Layout (JOL)](https://openjdk.org/projects/code-tools/jol/) since its 0.17, which is licensed under the GNU General Public License (GPL), Version 2. GPL2 is not compatible with Embulk because Embulk's executables are licensed under the Apache License 2.0.

In fact, Embulk used only a very limited part of Airlift's Slice that does not require JOL. We decided to copy only the required part of Slice, which is licensed under the Apache License 2.0. Slice has been removed (#2) as a dependency from Embulk since [Embulk v0.10.46](https://github.com/embulk/embulk/releases/tag/v0.10.46).

Netty's Buffer
---------------

[Netty](https://github.com/netty/netty)'s Buffer has been used since the very beginning of Embulk to manipulate byte buffers in `org.embulk.spi.Buffer`.

Netty's Buffer has been hidden (#1) behind `embulk-deps` since [Embulk v0.9.22](https://github.com/embulk/embulk/releases/tag/v0.9.22).

FindBugs
---------

[FindBugs](https://findbugs.sourceforge.net/) was used to build and test the Embulk core, and FindBugs' annotations were incorporated into the Embulk core.

However, [FindBugs is no longer maintained.](https://mailman.cs.umd.edu/pipermail/findbugs-discuss/2016-November/004321.html) [SpotBugs](https://spotbugs.github.io/) is a successor, and could be a good alternative to FindBugs, but it did not seem worth dirtying the dependencies with its annotation library.

FindBugs has been removed (#2) since [Embulk v0.9.18](https://github.com/embulk/embulk/releases/tag/v0.9.18).

SLF4J
------

[SLF4J](https://www.slf4j.org/) is the de-facto standard logging framework in Java, which has been common throughout the Embulk core and plugins.

The standard Java logging (`java.util.logging`) is unfortunately not very useful, then almost all existing open-source Java libraries do not use the standard logging. We have to accept some external dependencies for logging.

SLF4J has long been the standard in the Java eco-system. No critical security issues like [Log4Shell in Apache Log4j **2**](https://logging.apache.org/log4j/2.x/security.html#CVE-2021-44228) have been reported about SLF4J so far.

SLF4J is kept (#3) in the Embulk core up until now and from here on out.

Logback
--------

[Logback](https://logback.qos.ch/) is a popular logging driver behind SLF4J that has been used in Embulk for a long time.

Good Java libraries should not directly depend on Logback. Dependency conflicts between the Embulk core and plugins would be unlikely to occur on Logback. On the other hand, there is no proactive reason to keep loading Logback in the top-level class loader.

Logback is kept, but to be hidden from plugins (#4) in the future. Someday we may consider some kind of Embulk-specific logging mechanism for each plugin, to be more context-aware.

MessagePack for Java
---------------------

[MessagePack for Java](https://github.com/msgpack/msgpack-java) has been used to represent the JSON Column Type in Embulk since [Embulk v0.8.0](https://github.com/embulk/embulk/releases/tag/v0.8.0).

MessagePack for Java has caused dependency conflict deadlock as explained above for Jackson and Guava. [`embulk-parser-msgpack`](https://github.com/embulk/embulk-parser-msgpack) was unable to use later versions of MessagePack for Java. The Embulk core was not able to easily upgrade its own MessagePack for Java because an upgrade might break compatibility with some Embulk plugins.

MessagePack for Java is kept for a while, but will eventually be removed (#4). The JSON Column Type will be replaced by `org.embulk.spi.json.JsonValue` added in the Embulk SPI. [EEP-2: JSON Column Type](./eep-0002.md) explains the milestones for the replacement.

Compatibility and Migration
=============================

**The plugin compatibility is broken.** That is the decision.

We had the Embulk v0.10 series for the migration period as explained above in the "Problems" section. Plugins need explicit migration to catch up. We also announced for plugins how to catch up with Embulk v0.10, v0.11, and the upcoming v1.0.

* [For Embulk plugin developers: Get ready for v0.11 and v1.0!](https://dev.embulk.org/topics/get-ready-for-v0.11-and-v1.0-updated.html)

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
