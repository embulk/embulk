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

The Embulk core has to do a certain amount of work by itself, then it has several library dependencies for its own use. For example, [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) for parsing the user's Embulk configuration YAML, [Jackson](https://github.com/FasterXML/jackson) for processing user configurations, [Guice](https://github.com/google/guice) for managing its own object lifecycles, [Guava](https://github.com/google/guava) for utilities, [SLF4J](https://www.slf4j.org/) and [Logback](https://logback.qos.ch/) for logging, ...

On the other hand, almost all the Embulk plugins need their own library dependencies with transitive dependencies for their own purposes. For example, some AWS-related plugins need [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html), [whose v1 transitively needed Jackson](https://central.sonatype.dev/artifact/com.amazonaws/aws-java-sdk-core/1.12.385/dependencies). Some Google Cloud-related plugins require [Google Cloud Client Library for Java](https://github.com/googleapis/google-cloud-java), [which requires Jackson and Guava transitively](https://central.sonatype.dev/artifact/com.google.cloud/google-cloud-storage/2.17.1/dependencies).

The Embulk core assumes that it will be loaded by the top-level class loader (a.k.a. [system class loader or application class loader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassLoader.html#builtinLoaders)). An Embulk plugin is loaded by another class loader that has the top-level class loader as its parent.

The dependencies of an Embulk plugin may have conflicted with the dependencies of the Embulk core. This caused a dependency "deadlock" until Embulk v0.9. Upgrading a dependency library in the Embulk core could break some Embulk plugins. Upgrading a dependency library in some Embulk plugins was not possible without upgrading in the Embulk core. Upgrading all the Embulk plugins in the world was uneasy because all the Embulk plugins are not maintained by the Embulk project, but by volunteer developers in the world. Even if such an upgrade was achieved once, another upgrade would come again and again.

This was the main purpose of the "development" v0.10 series. It was the main reason why a lot of incompatibilities were introduced between Embulk v0.9 and v0.11 through Embulk v0.10.0 - v0.10.50.

This EEP presents an overview of the changes in the class loading and dependency libraries through Embulk v0.10.0 - v0.10.50, and explains why the changes were necessary.

Approaches: against the conflicts
==================================

In order to deal with the class loading and dependency problems, we adopted four types of approaches depending on the characteristics of each dependency.

1. Hide
2. Remove
3. Keep
4. Keep, but to be removed or hidden in the future

Many of the dependency libraries are still used even in Embulk v0.11, but hidden (#1) behind yet another child class loader. In Java's class loading mechanism, a class that is loaded by a child class loader can find a class that is loaded by its parent class loader, but cannot find a class that is loaded by its "sibling" class loader. Those dependency libraries are loaded by the other child class loader. Embulk plugins cannot find those hidden classes of those dependency libraries so that they would not conflict. Those hidden dependency libraries are bundled as `embulk-deps`, yet another artifact of the Embulk core. Mechanisms to hide dependencies behind `embulk-deps` would be explained in another upcoming EEP.

Some of the dependency libraries are just removed (#2) if its use was not very critical, or if it was difficult to keep it even in `embulk-deps` unfortunately.

Embulk plugins had expected that those dependency libraries were "there" loaded in the parent class loader until Embulk v0.9. Hiding or removing them brought some critical incompatibilities. However, the dependency "deadlock" issue could not have been resolved while those dependency libraries were visible from Embulk plugins. We concluded that the incompatibility was unavaoidable to resolve the deadlock problem, and decided to "go" with it through the Embulk v0.10 "development" series.

One of the dependency libraries (SLF4J) is kept (#3) up until now and from here on out.

Two of the dependency libraries (MessagePack for Java and Logback) are kept for a while, but planned to be removed (#4).

Walkthrough: each library
==========================

Let's walk through each library to analyze, discuss, and explain the design decisions for each case.

Jackson
--------

Jackson had been causing the biggest dependency conflicts. Let's start from Jackson.

[Jackson](https://github.com/FasterXML/jackson) consists of three core modules and a lot of add-on modules. They are distinct Maven artifacts with dependencies. For example :

* [`jackson-databind`](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind) depends on [`jackson-core`](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-core) and [`jackson-annotations`](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-annotations).
* [`jackson-datatype-jdk8`](https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jdk8) depends on `jackson-databind` and `jackson-core`.
* [`jackson-module-guice`](https://central.sonatype.com/artifact/com.fasterxml.jackson.module/jackson-module-guice) depends also on `jackson-databind`, `jackson-core`, and `jackson-annotations` along with [Guice](https://github.com/google/guice) and [Guava](https://github.com/google/guava).

Jackson does not expect to combine different versions of its modules. For example, `jackson-datatype-jdk8:2.15.3` is expected to be loaded with `jackson-core:2.15.3`. Nobody knows what happens if `jackson-datatype-jdk8:2.15.3` is loaded with `jackson-core:2.6.7`.

The Embulk core has used `jackson-core`, `jackson-annotations`, `jackson-databind`, `jackson-datatype-jdk8`, and some more add-on modules. Their versions has been `2.6.7`.

What if an Embulk plugin needs to use `jackson-dataformat-cbor:2.13.5` at least? The Embulk core has already loaded `jackson-core:2.6.7`, `jackson-annotations:2.6.7`, `jackson-databind:2.6.7`, and `jackson-datatype-jdk8:2.6.7` in its top-level class loader, which cannot be cancelled. The Embulk plugin wants `jackson-dataformat-cbor:2.13.5`. They... do not match.

In contrast, what if the Embulk core upgrades its own Jackson dependencies to `2.15.3`? There might have been an Embulk plugin that uses `jackson-dataformat-cbor:2.6.7` to be coupled with the existing Embulk core. This plugin could be broken if the Embulk core uptrades its own Jackson dependencies to `2.15.3`. They do not match, too.

This is the "deadlock". The deadlock could be resolved by upgrading all the dependencies of the core and plugins simultaneously, but all the plugins are not maintained by the Embulk project, but by voluntary developers. Upgrades would be lagged and misaligned. Furthermore, even if such a simultaneous upgrade is achieved, next upgrades would come soon again and again. Repeating such upgrades would not work in the community.

Finally, we concluded to make Jackson of the Embulk core to be somehow invisible from plugins so that the Embulk core and plugins can upgrade their own dependencies by their own decisions, at the timing of their own, without any deadlock. It would naturally bring critical incompatibilities, but we would have to accept it. It couldn't be helped.

Jackson has still been required for processing user configurations in the Embulk core. Jackson cannot be just removed. Then, Jackson libraries are "hidden" (#1) in `embulk-deps` as of [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32).

All the Embulk plugins have had to process user configurations. Many Embulk plugins have used Jackson for some more purposes. [Embulk plugins are asked to have their own Jackson dependencies by themselves to work with Embulk v0.10.](https://dev.embulk.org/topics/get-ready-for-v0.11-and-v1.0-updated.html) User configurations are expected to be processed in plugins by [`embulk-util-config`](https://github.com/embulk/embulk-util-config) which uses Jackson in it.

The core would get more compact, and plugins would take more responsibilities. This is also a part of [the "Compact Core" Principle in EEP-3](./eep-0003.md).

SnakeYAML
----------

[SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) is a YAML processor, which Embulk uses for parsing user configuration YAML.

SnakeYAML has been used only for parsing user configurations. User configurations have been converted to Jackson objects once SnakeYAML parses the user configuration YAML. Almost all the Embulk plugins are not expected to use SnakeYAML by themselves.

SnakeYAML has been "hidden" (#1) in `embulk-deps` since [Embulk v0.9.23](https://github.com/embulk/embulk/releases/tag/v0.9.23) without big impacts expected.

Java Bean Validation API and Apache BVal
-----------------------------------------

[The Java Bean Validation](https://jcp.org/en/jsr/detail?id=303) had been used with [Apache BVal](https://bval.apache.org/) to validate Embulk's user configurations, such as "must not be empty", "must be a positive integer", and so on.

User configurations are, however, processed in plugins with `embulk-util-config` in Embulk v0.10 as explained above for Jackson. The Embulk core no longer needs to have the Java Bean Validation API and Apache BVal in it.

In addition, the Java Bean Validation API has been impacted by the [transition from Java EE to Jakarta EE](https://blogs.oracle.com/javamagazine/post/transition-from-java-ee-to-jakarta-ee). Even if the Embulk core provides the Validation API (`javax.validation.*`) for plugins, the `javax.validation.*` packages would no longer be maintained. It has to be migrated to the new Jakarta Bean Validation API (`jakarta.validation.*`) someday.

Finally, Java Bean Validation API and Apache BVal need [Java Architecture for XML Binding (JAXB), which has been removed from the Jvaa SE since Java 11](https://openjdk.org/jeps/320). The Embulk core would have to contain JAXB dependencies to keep Java Bean Validation API and Apache BVal while JAXB is also migrated from `javax.xml.*` to `jakarta.xml.*` as well.

They are too much complicated to keep. The Java Bean Validation API and Apache BVal were once hidden behind `embulk-deps` in [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32), but then just simply removed (#2) from the Embulk core eventually as of [Embulk v0.10.49](https://github.com/embulk/embulk/releases/tag/v0.10.49).

Guice
------

Dependency injection by [Guice](https://github.com/google/guice) had laid down its roots deep in the Embulk core architecture. However, the actual use of Guice in Embulk was not the real dependency injection, but so-called [the service locator anti-pettern](https://blog.ploeh.dk/2010/02/03/ServiceLocatorisanAnti-Pattern/). The [`Injector`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Injector.html) instance was dragged around and stored everywhere. The [`injector.getInstance(....class)`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Injector.html#getInstance-java.lang.Class-) calls were spawling everywhere, not only when constructing instances, but as the need arises on demand at any runtime. [The control was not actually inversed](https://en.wikipedia.org/wiki/Inversion_of_control), but the dependencies were interweaved randomly among the entire Embulk architecture.

Should it have been fixed into the "real dependency injection" with Guice? No, actually. At the bottom line, Embulk is designed basically for one-shot execution with one Java process. No lifecycle management is required. Embulk has no strong demand for dependency injection containers. (JFYI, users could tweak `EmbulkEmbed` for multiple executions with one Java process, but it could easily cause other unavoidable problems such as class loader leaks. It is not recommended.)

Furthermore, Guice drags Guava (explained below), which is often a source of unintended incompatibility.

Hiding Guice behind `embulk-deps` is not trivial. And, Guice in the top-level class loader involves Guava also in the top-level class loader. Guice is removed (#2) from the Embulk core as of [Embulk v0.10.33](https://github.com/embulk/embulk/releases/tag/v0.10.33).

Guava
------

[Guava](https://github.com/google/guava) is a famous standard-ish Java library that includes utility classes such as immutable collections. The Embulk core had included Guava. On the other hand, Guava is known to often introduce breaking changes between versions.

* [Compatibility (google/guava)](https://github.com/google/guava/wiki/Compatibility)
* [Bug 427862 - Incompatible Guava versions/dependencies used by projects (eclipse.org)](https://bugs.eclipse.org/bugs/show_bug.cgi?id=427862)

Deadlock problems similar to Jackson (see above) would happen. The Embulk core had used Guava 18.0. Guava 18.0 was on the top-level class loader, which cannot be cancelled from an Embulk plugin. An Embulk plugin couldn't use a later version of Guava. On the other hand, upgrading Guava in the Embulk core could break some Embulk plugins due to Guava's incompatibility. It's the deadlock, too.

Like Jackson's case, we had to make Guava of the Embulk core to be invisible from plugins, too. Unlike Jackson's case, Guava was not mandatory once Guice had gone. Almost all the use-cases of Guava could be simply replaced with Java 8 standard.

Guava is just removed (#2) as of [Embulk v0.10.39](https://github.com/embulk/embulk/releases/tag/v0.10.39).

JRuby
------

[JRuby](https://www.jruby.org/) had been mixed in the top-level class loader. Not only JRuby by itself, JRuby contained a lot of its own dependency libraries like Joda-Time (explained below).

As explained in [EEP-6: JRuby as Optional](./eep-0006.md), JRuby has been optional since [Embulk v0.10.21](https://github.com/embulk/embulk/releases/tag/v0.10.21) under yet another child class loader.

Joda-Time
----------

[Joda-Time](https://www.joda.org/joda-time/) had been used in the Embulk core for representing and calculating timestamps (date and time), but Joda-Time is no longer maintained actively. Users of Joda-Time are asked to migrate to `java.time` (JSR-310), which is in a core part of the JDK as of Java 8.

Joda-Time had been obsolete in Embulk along with making JRuby as optional (see above), and removed (#2) at all as of [Embulk v0.10.30](https://github.com/embulk/embulk/releases/tag/v0.10.30).

ICU4J
------

[ICU4J](https://unicode-org.github.io/icu/userguide/icu4j/) (The International Components for Unicode for Java) had been used in the Embulk core only for detecting character sets in Guess plugins.

ICU4J could potentially be a source of dependency conflicts between the Embulk core and plugins, but it unlikely happened as many plugins do not depend on ICU4J.

While on the other hand, ICU4J is so huge. For example, `icu4j-54.1.1.jar` is ~10.6MB, `icu4j-57.2.jar` is ~10.8MB, and `icu4j-74.2.jar` is ~13.6MB. ICU4J occupied a quarter or a half in Embulk distribution packages while it was used only for detecting character sets. We doubted if it was really worth having the whole ICU4J dependency in the Embulk core.

Finally, ICU4J is removed (#2) from the Embulk core as of [Embulk v0.10.45](https://github.com/embulk/embulk/releases/tag/v0.10.45). Instead, we copied classes for character set detection from ICU4J 57.2 into [`embulk-util-guess`](https://github.com/embulk/embulk-util-guess). They are licensed under the ICU License, which is considered compatible as the X license, hence the MIT License. Guess plugins that need to detect character sets would use `embulk-util-guess` by themselves.

Apache Maven
-------------

[Apache Maven](https://maven.apache.org/) has been used in the Embulk core for [EEP-5: Maven-style Plugins](./eep-0005.md) since [Embulk v0.9.8](https://github.com/embulk/embulk/releases/tag/v0.9.8), and they have been hidden (#1) behind a child class loader since [Embulk v0.9.15](https://github.com/embulk/embulk/releases/tag/v0.9.15).

Apache Velocity
----------------

[Apache Velocity](https://velocity.apache.org/) had been used in the Embulk core to "scaffold" the template plugin source code by the `embulk new` subcommand since [Embulk v0.8.19](https://github.com/embulk/embulk/releases/tag/v0.8.19), and it had been hidden behind a child class loader since [Embulk v0.9.17](https://github.com/embulk/embulk/releases/tag/v0.9.17).

Finally, it has been removed (#2) along with removing the `embulk new` subcommand as of [Embulk v0.10.37](https://github.com/embulk/embulk/releases/tag/v0.10.37).

Apache Commons CLI
-------------------

[Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) has been used in the Embulk core for parsing command line options since [Embulk v0.8.28](https://github.com/embulk/embulk/releases/tag/v0.8.28), and it has been hidden (#1) behind a child class loader since [Embulk v0.9.17](https://github.com/embulk/embulk/releases/tag/v0.9.17).

Apache Commons Lang 3
----------------------

[Apache Commons Lang](https://commons.apache.org/proper/commons-lang/) 3 has been used in the Embulk core for transitive dependencies from other Apache libraries such as Apache BVal, Apache Maven, and Apache Commons CLI.

Apache Commons Lang 3 has been finally hidden (#1) behind `embulk-deps` as of [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32) along with hiding Apache BVal. It is still there because of Apache Maven and Apache Commons CLI although Apache BVal is removed as of [Embulk v0.10.49](https://github.com/embulk/embulk/releases/tag/v0.10.49).

Airlift's Slice
----------------

[Airlift's Slice](https://github.com/airlift/slice) library had been used in the Embulk core since the very beginning to manipulate Java byte arrays for packing/unpacking Embulk's column values in `org.embulk.spi.Page`.

Airlift's Slice could potentially be a source of dependency conflicts between the Embulk core and plugins, but it unlikely happened as many plugins do not depend on Slice.

While on the other hand, Airlift's Slice has started to depend on [OpenJDK's Java Object Layout (JOL)](https://openjdk.org/projects/code-tools/jol/) since its 0.17, which is licensed under the GNU General Public License (GPL), Version 2. It would not be compatible with Embulk as Embulk's executable binaries are licensed as the Apache License 2.0.

Indeed, Embulk has used only a very limited part of Airlift's Slice, which does not require JOL. We copied only the required part into the Embulk core from Slice that is licensed under the Apache License 2.0, and removed (#2) Slice as a dependency from Embulk as of [Embulk v0.10.46](https://github.com/embulk/embulk/releases/tag/v0.10.46).

Netty's Buffer
---------------

[Netty](https://github.com/netty/netty)'s Buffer library has been used in the Embulk core since the very beginning to manipulate byte buffers in `org.embulk.spi.Buffer`.

Netty's Buffer is hidden (#1) behind `embulk-deps` as of [Embulk v0.9.22](https://github.com/embulk/embulk/releases/tag/v0.9.22).

FindBugs
---------

[FindBugs](https://findbugs.sourceforge.net/) had been used in building/testing Embulk, and FindBugs' annotations had been included in the Embulk core.

However, [FindBugs is no longer maintained.](https://mailman.cs.umd.edu/pipermail/findbugs-discuss/2016-November/004321.html) [SpotBugs](https://spotbugs.github.io/) is a successor, and could be a good alternative to FindBugs, but it did not seem worth dirtying the dependencies by its annotation library.

FindBugs is removed (#2) as of [Embulk v0.9.18](https://github.com/embulk/embulk/releases/tag/v0.9.18).

SLF4J
------

[SLF4J](https://www.slf4j.org/) has been the common logging framework throughout the Embulk core and plugins.

Java's standard logging (`java.util.logging`) is not very useful unfortunately, and almost all the existing Java open-source librarires do not use the standard logging. We have to accept some external dependency there in logging.

SLF4J seems stable enough for a long time, and which has been the de-facto standard logging framework in the Java eco-system. No critical security issues, such as [Log4Shell in Apache Log4j **2**](https://logging.apache.org/log4j/2.x/security.html#CVE-2021-44228), have been reported on SLF4J so far.

SLF4J is kept (#3) up until now and from here on out.

Logback
--------

[Logback](https://logback.qos.ch/) is the logging driver behind SLF4J that has been used in Embulk also for a long time.

Good-mannered Java libraries should not depend directly on Logback. Dependency conflicts would unlikely happen on Logback between the Embulk core and plugins. However, at the same time, there is no proactive reason to keep loading Logback in the top-level class loader.

Logback is kept, but to be hidden from plugins (#4) in the future. Someday, we may consider a kind of Embulk-specific logging mechanism for each plugin to be more context-aware.

MessagePack for Java
---------------------

[MessagePack for Java](https://github.com/msgpack/msgpack-java) has been used to represent the JSON Column Type in Embulk since [Embulk v0.8.0](https://github.com/embulk/embulk/releases/tag/v0.8.0).

MessagePack for Java has caused dependency conflict deadlock like Jackson and Guava explained above. [`embulk-parser-msgpack`](https://github.com/embulk/embulk-parser-msgpack) was not able to use later versions of MessagePack for Java. The Embulk core was not able to upgrade its own MessagePack for Java easily because an upgrade might have broken compatibility with some Embulk plugins.

MessagePack for Java is kept for a while, but to be removed (#4) eventually. The JSON Column Type is to be replaced with `org.embulk.spi.json.JsonValue` added in the Embulk SPI. [EEP-2: JSON Column Type](./eep-0002.md) explains the milestones for the replacement.

Compatibility and Migration
=============================

**The plugin compatibility is broken.** That is the decision.

We had the Embulk v0.10 series for the migration period as explained above in the "Problems" section. Plugins needed explicit migration for catch-up. We also announced for plugins how to catch up with Embulk v0.10, v0.11, and upcoming v1.0.

* [For Embulk plugin developers: Get ready for v0.11 and v1.0!](https://dev.embulk.org/topics/get-ready-for-v0.11-and-v1.0-updated.html)

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
