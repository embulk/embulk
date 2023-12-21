---
EEP: unnumbered
Title: Core Dependencies and Class Loading
Author: dmikurube
Status: Draft
Type: Standards Track
Created: 2023-12-21
---

Problem: dependency conflicts
==============================

The Embulk core has to do a certain amount of work by itself, then it had several library dependencies for its own use. For example, [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) for parsing user's Embulk configuration YAML, [Jackson](https://github.com/FasterXML/jackson) for processing user configurations, [Guice](https://github.com/google/guice) for managing its own object life cycles, [Guava](https://github.com/google/guava) for utilities, [SLF4J](https://www.slf4j.org/) and [Logback](https://logback.qos.ch/) for logging, ...

On the other hand, almost all the Embulk plugins need their own library dependencies with transitive dependencies for their own purposes. For example, some AWS-related plugins need [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html), [whose v1 has needed Jackson transitively](https://central.sonatype.dev/artifact/com.amazonaws/aws-java-sdk-core/1.12.385/dependencies). Some Google Cloud-related plugins need [Google Cloud Client Library for Java](https://github.com/googleapis/google-cloud-java), [which has needed Jackson and Guava transitively](https://central.sonatype.dev/artifact/com.google.cloud/google-cloud-storage/2.17.1/dependencies).

The Embulk core assumes to be loaded by the top-level class loader (a.k.a. [system class loader or application class loader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassLoader.html#builtinLoaders)). An Embulk plugin is loaded by another class loader that has the top-level class loader as its parent.

The dependencies of an Embulk plugin could have conflicted with the dependencies of the Embulk core. It had caused a dependency "deadlock" until Embulk v0.9. Upgrading a dependency library in the Embulk core might have broken some Embulk plugins. Upgrading a dependency library in some Embulk plugins could not have been achieved without upgrading in the Embulk core. Upgrading all the Embulk plugins in the world was uneasy because all Embulk plugins were not maintained by the Embulk project, but by voluntary developers. Even if such an upgrade was once achieved, another upgrade would come again and again.

It was the biggest purpose of the "development" v0.10 series. It was the main reason why a lot of incompatibilities were introduced between Embulk v0.9 and v0.11 through Embulk v0.10.0 - v0.10.50.

This EEP presents an overview of the changes in class loading and dependency libraries through Embulk v0.10.0 - v0.10.50, and explains why the changes were needed.

Approaches
===========

In order to deal with the class loading and dependency problems, we adopted four types of approaches depending on the characteristics of each dependency.

1. Hide
2. Remove
3. Keep
4. Keep, but to be removed in the future

Many of the dependency libraries are still used even in Embulk v0.11, but hidden (#1) behind yet another child class loader. In Java's class loading mechanism, a class that is loaded by a child class loader can find a class that is loaded by its parent class loader, but cannot find a class that is loaded by its "sibling" class loader. Those dependency libraries are loaded by the other child class loader. Embulk plugins cannot find those hidden classes of those dependency librarires so that they would not conflict. Those hidden dependency libraries are bundled as `embulk-deps`, yet another artifact of the Embulk core.

Some of the dependency libraries are just removed (#2) if its use was not very critical, or if it was difficult to keep it even in `embulk-deps` unfortunately.

Embulk plugins had expected that those dependency libraries were "there" loaded in the parent class loader until Embulk v0.9. Hiding or removing them brought some critical incompatibilities. However, the dependency "deadlock" issue could not have been resolved while those dependency libraries were visible from Embulk plugins. We concluded that the incompatibility was unavaoidable to resolve the deadlock problem, and decided to "go" with it through the Embulk v0.10 "development" series.

One of the dependency libraries (SLF4J) is kept (#3) up until now and from here on out.

Two of the dependency libraries (MessagePack for Java and Logback) are kept for a while, but planned to be removed (#4).

Walkthrough: libraries
=======================

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

[The Java Bean Validation](https://jcp.org/en/jsr/detail?id=303) has been used with [Apache BVal](https://bval.apache.org/) to validate Embulk's user configurations, such as "must not be empty", "must be a positive integer", and so on.

User configurations are, however, processed in plugins with `embulk-util-config` in Embulk v0.10 as explained above for Jackson. The Embulk core no longer needs to have the Java Bean Validation API and Apache BVal in it.

In addition, the Java Bean Validation API has been impacted by the [transition from Java EE to Jakarta EE](https://blogs.oracle.com/javamagazine/post/transition-from-java-ee-to-jakarta-ee). Even if the Embulk core provides the Validation API (`javax.validation.*`) for plugins, the `javax.validation.*` packages would no longer be maintained. It has to be migrated to the new Jakarta Bean Validation API (`jakarta.validation.*`) someday.

Finally, Java Bean Validation API and Apache BVal need [Java Architecture for XML Binding (JAXB), which has been removed from the Jvaa SE since Java 11](https://openjdk.org/jeps/320). The Embulk core would have to contain JAXB dependencies to keep Java Bean Validation API and Apache BVal while JAXB is also migrated from `javax.xml.*` to `jakarta.xml.*` as well.

They are too much complicated to keep. The Java Bean Validation API and Apache BVal were once hidden behind `embulk-deps` in [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32), but then just simply removed (#2) from the Embulk core eventually as of [Embulk v0.10.49](https://github.com/embulk/embulk/releases/tag/v0.10.49).

Guice
------

Dependency injection by [Guice](https://github.com/google/guice) had laid down its roots deep in the Embulk core architecture. However, the actual use of Guice in Embulk was not the real dependency injection, but so-called [the service locator anti-pettern](https://blog.ploeh.dk/2010/02/03/ServiceLocatorisanAnti-Pattern/). The [`Injector`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Injector.html) instance was dragged around and stored everywhere. The [`injector.getInstance(....class)`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Injector.html#getInstance-java.lang.Class-) calls were spawling everywhere, not only when constructing instances, but as the need arises on demand at any runtime. [The control was not actually inversed](https://en.wikipedia.org/wiki/Inversion_of_control), but the dependencies were interweaved randomly among the entire Embulk architecture.

Should it have been fixed into the "real dependency injection" with Guice? No, actually. At the bottom line, Embulk is designed basically for one-shot execution with one Java process. No lifecycle management is required. Embulk has no strong demand for dependency injection containers. (JFYI, users could tweak `EmbulkEmbed` for multiple executions with one Java process, but it could easily cause other unavoidable problems such as class loader leaks. It is not recommended.)

Furthermore, Guice drags Guava (explained below), which is often a source of unintended incompatibility.

Hiding Guice behind [`embulk-deps`] is not trivial. And, Guice in the top-level class loader involves Guava also in the top-level class loader. Guice is removed (#2) from the Embulk core as of [Embulk v0.10.33](https://github.com/embulk/embulk/releases/tag/v0.10.33).

Guava
------

[Guava](https://github.com/google/guava) is a standard-ish Java utility library that includes immutable collections and else.

JRuby
------

Joda-Time
----------

Apache Commons Lang
--------------------

Airlift's Slice
----------------

Netty's Buffer
---------------

ICU4J
------

Findbugs
---------

SLF4J
------

Logback
--------

MessagePack for Java
---------------------

Class Loader Tweaks
====================

Alternatives considered
------------------------

Migration
==========


Plugin SPI Changes
===================


Backwards Compatibility
========================


Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
