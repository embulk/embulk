---
EEP: unnumbered
Title: SPI Separation for Compatibility Contract
Author: dmikurube
Status: Draft
Type: Standards Track
---

Introduction
=============

Embulk v0.9 and earlier had only the `embulk-core` main artifact, except for `embulk-deps` which started in v0.9.15 for Apache Maven. (See [EEP-7: Core Library Dependencies](./eep-0007.md) and [EEP-9: Class Loading, and Executable JAR File Structure](./eep-0009.md) for `embulk-deps`.) Embulk plugins had been built depending on `embulk-core`. This allowed Embulk plugins to access any class in the Embulk core, and caused restrictions that should not be necessary in the Embulk core.

The Embulk core had to expect that any class in the core could be called by some Embulk plugin because changing anything in the core could cause an incompatible change. This discouraged the maintainers from making changes to the Embulk core.

Since v0.10, Embulk has clearly specified its [SPI (Service Provider Interface)](https://en.wikipedia.org/wiki/Service_provider_interface) for plugins, so that plugins would not have unnecessary dependencies on the details of the Embulk core implementation, and the maintainers could limit compatibility concerns. The SPI acts as a compatibility contract with plugins. We only care about compatibility with the SPI classes. The core classes outside the SPI may have incompatible changes at any time without notice.

This was another issue related to [EEP-3: The "Compact Core" Principle](./eep-0003.md) inside the Embulk core codebase, in contrast to [EEP-7](./eep-0007.md), which was for dependency libraries.

This EEP explains the details about the Embulk SPI separation.

Separation by Maven Artifact
=============================

All the Embulk core classes had been included equally in the `embulk-core` Maven artifact. Every Embulk plugin implicitly depended on `embulk-core` like [the `provided` scope](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope). (See the example `build.gradle` below.)

```gradle
// An example from embulk-input-s3:v0.3.5
// https://github.com/embulk/embulk-input-s3/blob/v0.3.5/build.gradle
configurations {
    provided
    // ...
}

dependencies {
    compile  "org.embulk:embulk-core:0.9.12"
    provided "org.embulk:embulk-core:0.9.12"
    // ...
}

task classpath(type: Copy, dependsOn: ["jar"]) {
    // ...
    from (configurations.runtime - configurations.provided + files(jar.archivePath))
    // ...
}
```

It was definitely hard for plugin developers to tell which class was officially provided for plugins, or not expected to be accessed by plugins, because they were equally accessible. It was also hard for the Embulk core maintainers to find an Embulk plugin accessing a core class that was not expected to be accessed by a plugin.

We decided to have the `embulk-spi` Maven artifact split from `embulk-core` so that an Embulk plugin could explicitly depend only on the official Embulk SPI (`embulk-spi`). The core maintainers do not consider non-SPI classes need to be compatible.

```gradle
dependencies {
    compileOnly "org.embulk:embulk-spi:0.11"

    // An Embulk plugin should no longer depend on "embulk-core".
}
```

With this separation, plugin developers and the core maintainers can easily confirm that an Embulk plugin only depends on the official Embulk SPI.

Loophole for accessing `embulk-core`
-------------------------------------

We have not introduced any strict access control to core classes from a plugin, despite the need for separation. Technically, an Embulk plugin can still depend on `embulk-core` and access the core classes.

Some plugins might still need such an access to `embulk-core` classes for compatibility and consistency for a while. They can still access `embulk-core` classes by having a dependency on `embulk-core`. This is a loophole, but acceptable.

The plugin developer and the core maintainers can easily be aware that the plugin is exploiting the loophole because it has an explicit dependency on `embulk-core`. This would be sufficient for further investigation.

Inclusion of `embulk-spi` classes in `embulk-core`
---------------------------------------------------

The `embulk-core` JAR file includes `embulk-spi` classes extracted in it, not just depends on `embulk-spi`.

This is to prepare for the Java module system in Java 9 and later. The Java module system does not accept the "split packages" situation, in which a Java package and two different JARs each have a class in that package. (See "Migrating Your Library to Java Modules" in [Java Magazine, July/August 2018](https://www.oracle.com/a/ocom/docs/corporate/java-magazine-jul-aug-2018.pdf), pp.53-64, by Nicolai Parlog for handling split packages in the Java module system.)

This means that all `org.embulk.spi` classes must be contained in the same single JAR file. On the other hand, some existing `org.embulk.spi` classes had to move to `embulk-spi` while others had to stay in `embulk-core`. At the same time, they also had to stay in the same Java package for compatibility.

We decided to have all `embulk-spi` classes extracted in the `embulk-core` JAR file so that both of the above requirements are met together. The `embulk-spi` classes are duplicated between the `embulk-spi` JAR file and the `embulk-core` JAR file. In order to avoid conflicts when loading these JAR files, the `pom.xml` of `embulk-core` has a dependency on `embulk-spi` as `provided`.

```xml
<dependency>
<groupId>org.embulk</groupId>
<artifactId>embulk-spi</artifactId>
<version>0.11</version>
<scope>provided</scope>
</dependency>
```

Selection of SPI classes
=========================

The following classes and interfaces in `embulk-core` are selected for the Embulk SPI.

1. Interfaces for the main class of each plugin type (`*Plugin` interface such as `InputPlugin` and `ParserPlugin`)
2. Classes and interfaces that appear in the method signatures of #1, and recursively
3. Classes and interfaces that need to be shared between plugins

Most of these were simply moved to `embulk-spi`, but some needed special treatment. The rest of this section explains each case.

`org.embulk.spi.Buffer`
------------------------

The `org.embulk.spi.Buffer` class was an implementation for conveying byte sequences between plugins. We split this into an abstract class `org.embulk.spi.Buffer` in `embulk-spi`, and an implementation class `org.embulk.spi.BufferImpl` in `embulk-core`, so that the Embulk SPI would not have a detailed implementation.

`Buffer` is obviously used by the main classes of file-related plugins, such as File Input plugins and Parser plugins.

The original `Buffer` did not allow direct construction by its constructor. It must be allocated by `BufferAllocator` explained below. Making `Buffer` abstract did not cause any major compatibility problems.

Related changes:
* [Split `org.embulk.spi.Buffer` into a separate artifact `embulk-api` (embulk/embulk#1232)](https://github.com/embulk/embulk/pull/1232)
* [Get the constructor of org.embulk.spi.Buffer protected (embulk/embulk#1278)](https://github.com/embulk/embulk/pull/1278)
* [Declare Buffer's constructor compatible with v0.9's (embulk/embulk#1410)](https://github.com/embulk/embulk/pull/1410)

`org.embulk.spi.Page`
----------------------

The `org.embulk.spi.Page` class was an implementation for conveying row-column records between plugins. We split this into an abstract class `org.embulk.spi.Page` in `embulk-spi`, and an implementation class `org.embulk.spi.PageImpl` in `embulk-core`, so that the Embulk SPI would not have a detailed implementation.

`Page` is obviously used by the main classes of non-file plugins, such as Input, Output, and Filter plugins.

The original `Page` did not allow direct construction by its constructor. It must be built by `PageBuilder` explained below. Making `Page` abstract did not cause any major compatibility problems.

Related change:
* [Move org.embulk.spi.Page to embulk-api (embulk/embulk#1277)](https://github.com/embulk/embulk/pull/1277)

`org.embulk.spi.PageBuilder`
-----------------------------

The `org.embulk.spi.PageBuilder` class was an implementation for creating instances of `org.embulk.spi.Page`. We split this into an abstract-ish class `org.embulk.spi.PageBuilder` in `embulk-spi`, and an implementation class `org.embulk.spi.PageBuilderImpl` in `embulk-core`, so that the Embulk SPI would not have a detailed implementation.

`PageBuilder` does not appear in the method signatures of the plugin and related classes. However, it needed to be in the Embulk SPI so that the background buffer allocation mechanism is shared and orchestrated between plugins throughout the Embulk process.

`PageBuilder` was constructed by calling its own constructor in each plugin, but the constructor would not be available due to this separation. We have implemented a temporary constructor of `PageBuilder` to delegate to `PageBuilderImpl`. The temporary constructor will be available through the Embulk SPI v0.10 and v0.11, and will be removed at some point after v1.0.

Related change:
* [Move PageBuilder to embulk-api (embulk/embulk#1320)](https://github.com/embulk/embulk/pull/1320)

`org.embulk.spi.PageReader`
----------------------------

The `org.embulk.spi.PageReader` class was an implementation for interpreting `org.embulk.spi.Page` instances. We split this into an abstract-ish class `org.embulk.spi.PageReader` in `embulk-spi`, and an implementation class `org.embulk.spi.PageReaderImpl` in `embulk-core`, so that the Embulk SPI would not have a detailed implementation.

`PageReader` does not appear in the method signatures of the plugin and related classes. However, it needed to be in the Embulk SPI so that the background buffer allocation mechanism is shared and orchestrated between plugins throughout the Embulk process.

`PageReader` was constructed by calling its own constructor in each plugin, but the constructor would not be available due to this separation. We have implemented a temporary constructor of `PageReader` to delegate to `PageReaderImpl`. The temporary constructor will be available through the Embulk SPI v0.10 and v0.11, and will be removed at some point after v1.0.

Related change:
* [Move PageReader to embulk-api (embulk/embulk#1322)](https://github.com/embulk/embulk/pull/1322)

`org.embulk.spi.TempFileSpace`
-------------------------------

The `org.embulk.spi.TempFileSpace` class was an implementation for managing temporary files. We split this into an abstract class `org.embulk.spi.TempFileSpace` in `embulk-spi`, and an implementation class `org.embulk.spi.TempFileSpaceImpl` in `embulk-core`, so that the Embulk SPI would not have a detailed implementation.

`TempFileSpace` does not appear in the method signatures of the plugin and related classes. However, it needed to be in the Embulk SPI so that the temporary files are managed (typically cleaned up) in a unified manner throughout the Embulk process.

The original `TempFileSpace` did not allow direct construction by its constructor. It must be allocated by `Exec.getTempFileSpace()` explained below. Making `TempFileSpace` abstract did not cause any major compatibility problems.

Related change:
* [Move TempFileSpace to embulk-api (embulk/embulk#1318)](https://github.com/embulk/embulk/pull/1318)

`org.embulk.spi.Exec` and `org.embulk.spi.ExecSession`
-------------------------------------------------------

The `org.embulk.spi.Exec` class has been a static accessor for Embulk plugins to gain access to Embulk's internal APIs. The `org.embulk.spi.ExecSession` class was an instantiated implementation behind `Exec`. The `ExecSession` instance(s) are stored in [the Java thread-local storage](https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html) so that `Exec` can retrieve the corresponding `ExecSession` instance.

`Exec` is used in some way by almost all Embulk plugins, such as `Exec.getBufferAllocator()` and `Exec.getLogger()`, so it needed to be in the Embulk SPI. `ExecSession` needed to be in the SPI so that `Exec` can get the corresponding `ExecSession` instance. In addition, some plugins use `ExecSession` directly, although this is not recommended.

`Exec` is not instantiated. However, the original `Exec` contained some accessors for internal core functions, not for plugins, such as `Exec.doWith()`, `Exec.newPlugin()`, and `Exec.getModelManager()`. We split `Exec` into `org.embulk.spi.Exec` in `embulk-spi`, and `org.embulk.spi.ExecInternal` in `embulk-core` so that the internal core functions are not accessible from plugins. We also split `ExecSession` into `org.embulk.spi.ExecSession` and `org.embulk.spi.ExecSessionInternal` in the same way.

Related changes:
* [Prepare org.embulk.spi.ExecInternal to contain Exec methods not for plugins (embulk/embulk#1312)](https://github.com/embulk/embulk/pull/1312)
* [Move Exec and ExecSession to embulk-api (embulk/embulk#1327)](https://github.com/embulk/embulk/pull/1327)

Utility classes
----------------

Many classes were included under the `org.embulk.spi.*` Java packages, but most of them were just utility classes. They were not essential in the Embulk SPI, nor even in the Embulk core. We split them out as standalone external libraries to be used by each plugin, also as explained in [EEP-3](./eep-0003.md), like the examples below.

* `org.embulk.spi.json.JsonParser` is just a JSON parser, which does not appear in the method signatures of any plugin interface. We have externalized this as a standalone library [`embulk-util-json`](https://github.com/embulk/embulk-util-json) so that plugins can use it themselves.
* `org.embulk.spi.time.TimestampFormatter` and `org.embulk.spi.time.TimestampParser` are a timestamp formatter and parser, respectively, which do not appear in the method signatures of any plugin interface. We have externalized this as a standalone library [`embulk-util-timestamp`](https://github.com/embulk/embulk-util-timestamp) so that plugins can use it themselves. On the other hand, `org.embulk.spi.time.Timestamp` has been moved to the Embulk SPI as it is used to store TIMESTAMP data in `Page`.
* Classes under the `org.embulk.spi.util.*` Java packages are literally utilities. We have externalized them as standard libraries such as [`embulk-util-dynamic`](https://github.com/embulk/embulk-util-dynamic), [`embulk-util-file`](https://github.com/embulk/embulk-util-file), and [`embulk-util-text`](https://github.com/embulk/embulk-util-text).
* Classes and interfaces under the `org.embulk.config` Java package are tooling for processing Embulk's user configurations, typically specified in YAML. Some of them (e.g. `ConfigSource` and `TaskSource`) needed to be in the Embulk SPI because they are used to pass the configuration from the Embulk core to each plugin. However, we have externalized many of them (e.g. annotations like `@Config` and utilities to map YAML to each `PluginTask` class) as a standalone library [`embulk-util-config`](https://github.com/embulk/embulk-util-config) so that plugins can use it themselves. This separation was also necessary to hide Jackson from plugins, as clarified in [EEP-7](./eep-0007.md).

Deprecations
-------------

Along with the SPI and utility separation, we have deprecated and removed some classes, interfaces and methods. This section shows some examples.

### `org.embulk.spi.ExecSession.SessionTask`

We have removed the inner interface `org.embulk.spi.ExecSession.SessionTask` along with moving `ExecSession` to `embulk-spi`. It remains internally in `ExecSessionInternal` in `embulk-core`. `SessionTask` is deprecated as of Embulk v0.6.16.

Related change:
* [Prepare org.embulk.spi.ExecInternal to contain Exec methods not for plugins (embulk/embulk#1312)](https://github.com/embulk/embulk/pull/1312)

### `org.embulk.spi.type.Type`

We have deprecated `Type#getJavaType()` and `Type#getFixedStorageSize()` in `org.embulk.spi.type.Type` on this occasion as they have undergone some breaking changes while they were very internal things.

Related change:
* [Deprecate Type#getJavaType and Type#getFixedStorageSize (embulk/embulk#1325)](https://github.com/embulk/embulk/pull/1325)

### `org.embulk.config.DataSource` and its sub-interfaces

We have removed `DataSource#getAttributes()` and `DataSource#getObjectNode()` in `org.embulk.config.DataSource` and its sub-interfaces as they violated abstraction by leaking internal Jackson objects. They were also a blocker for hiding Jackson from plugins.

Related change:
* [Split some `org.embulk.config` classes into `embulk-api` (embulk/embulk#1237)](https://github.com/embulk/embulk/pull/1237)

### `org.embulk.config.CommitReport`

We have removed `org.embulk.config.CommitReport` as `org.embulk.config.TaskReport` has replaced `CommitReport`, and is deprecated as of Embulk v0.7.0. Related methods have been removed as well.

Related change:
* [Remove org.embulk.config.CommitReport (Fix #933) (embulk/embulk#1247)](https://github.com/embulk/embulk/pull/1247)

Others
-------

Many other classes and interfaces have simply moved to `embulk-spi`.

Maintaining the Embulk SPI
===========================

The Embulk SPI is versioned by two digits, such as `0.11`, `1.0`, and `1.1`, using a different strategy than the Embulk core. The versioning strategies are explained in [EEP-8: Milestones to Embulk v1.0, and versioning strategies to follow](./eep-0008.md).

To align with the versioning strategy, the Embulk SPI is maintained in a separate source repository at: [https://github.com/embulk/embulk-spi](https://github.com/embulk/embulk-spi)

Alternatives considered
========================

Separation of API and SPI
--------------------------

We started the Embulk SPI as two separate artifacts, `embulk-api` and `embulk-spi`, based on the following criteria.

* `embulk-api` contains accessors to Embulk's core functions. Utility libraries for Embulk plugins may also only depend on `embulk-api`, not `embulk-spi`, to allow Embulk plugins to access the core functions.
* `embulk-spi` consists of interfaces and abstract classes for Embulk plugins to inherit.

However, we finally merged them into `embulk-spi` in Embulk v0.10.44 because this separation did not contribute to clarity and understandability, but only increased complexity as far as we could observe.

Related change:
* [Merge embulk-api to embulk-spi (embulk/embulk#1566)](https://github.com/embulk/embulk/pull/1566)

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
