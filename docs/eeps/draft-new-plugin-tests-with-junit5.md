---
EEP: unnumbered
Title: Testing Embulk plugins with JUnit 5
Author: dmikurube
Status: Draft
Type: Standards Track
---

Motivation
===========

Embulk plugins have been tested with `embulk-junit4` (f.k.a. `embulk-test`) backed by JUnit 4, but JUnit 4 has not actively been maintained as of 2023. The JUnit team has been spending their major efforts on JUnit 5 for years. Embulk plugins have needed to catch up with JUnit 5.

JUnit 5 has many compatible features with JUnit 4, but the APIs of JUnit 5 are incompatible from JUnit 4. Therefore, `embulk-junit4` would never be compatible with JUnit 5. We need a brand new testing framework for Embulk plugins with JUnit 5!

On the other hand, the existing `embulk-junit4` has not been very easy to use, indeed. Building the brand new testing framework would be also a good chance for improvement.

This EEP explains the basic design of the new testing framework for Embulk plugins with JUnit 5, which is called `embulk-junit5`.

Goals
======

We aim the following points in `embulk-junit5` with improvements from `embulk-junit4`.

1. Being based on JUnit 5 (definitely)
2. Requiring no other Embulk plugins to test the target Embulk plugin
3. Reproducing the same class loading structure with the real Embulk's class loading
4. ...

The following section describes each in details.

Problems with embulk-junit4
============================

JUnit 4
--------

We have to catch up with JUnit 5, definitely, as mentioned in the Motivation section.

Other plugins
--------------

Testing with `embulk-junit4` often needed other plugins. For example, testing a File Input plugin (ex. S3 File Input) needed a CSV Parser plugin to confirm the input files are retrieved by the File Input plugin, and then processed in later stages properly.

However, they are unnecessary dependencies, which could bring unexpected impacts when the depended plugin changes. The CSV Parser has changed its internal structure around v0.11.0, and it actually made some impacts.

Class loading
--------------

In testing with `embulk-junit4`, all related classes from `embulk-core`, `embulk-deps`, the target plugin, and other depended plugins are loaded in the top-level test class loader.

It could definitely cause unintended results in testing. For example, while `embulk-deps` depends on `jackson-core:2.6.7`, the target plugin may depend on `jackson-core:2.15.3` and `jackson-datatype-guava:2.15.3`, and another depended plugin may depends on `jackson-core:2.9:10` and `jackson-datatype-guava:2.9.10`. Nobody could tell how the test results would be!

Design
=======

Run tests on a custom Test Engine
----------------------------------

The class loading structure is the biggest constraint to realize the goals of this testing framework. Test codes usually run on the top-level class loader of the testing environment while we wanted to run the test target (Embulk plugin) under a sub class loader.

JUnit 5 has a mechanism to build a custom "Test Engine" to run tests, apart from JUnit 5's standard "Jupiter" Test Engine and yet another standard "Vintage" Test Engine to run JUnit 3/4 tests on JUnit 5.

* See: [Test Engines from JUnit 5 User Guide (Version 5.10.1)](https://junit.org/junit5/docs/5.10.1/user-guide/#test-engines)

Reproducing the special class loading structure with JUnit 5 is tricky. Building a custom Test Engine seems too much at a glance, but haste makes waste. It would not require hacky things in every plugin's test code. It would not expect too much expertise on class loading for every plugin developer.

Split tests to simple unit tests and "integrated" Embulk plugin tests
----------------------------------------------------------------------

Simple unit tests, which just test the behavior of a single class, do not require the special class loading structure. The class loading structure is required only when testing its behavior as an Embulk plugin.

It is, however, uneasy to modify the class loading structure during a set of test execution. It would be reasonable to split tests into two parts:

1. Simple unit tests
2. "Integrated" Embulk plugin tests

The simple unit tests are common Java tests. On the other hand, the "integrated" Embulk plugin tests run with the special class loading structure. In terms of `embulk-junit4`, tests that required `@Rule EmbulkTestRuntime runtime` would be "integrated" Embulk plugin tests. Others would be simple unit tests.

Collaborate with Gradle and the Gradle plugin
----------------------------------------------

Ordinary Embulk plugins are built with Gradle and [the `org.embulk.embulk-plugins` Gradle plugin](https://plugins.gradle.org/plugin/org.embulk.embulk-plugins). A natural approach to distinguish the simple unit tests and the "integrated" Embulk plugin tests is to run them as two different Gradle test tasks. For example :

```
./gradlew test  # Run simple unit tests.
./gradlew embulkPluginTest  # Run the "integrated" Embulk plugin tests.
```

At the same time, Gradle with the Gradle plugin is the good point to tweak the class loading structure. Indeed, it is difficult to tweak the class loading structure once the test execution has started.

The `org.embulk.embulk-plugins` Gradle plugin would define a new `EmbulkPluginTest` task type that extends the standard [`Test`](https://docs.gradle.org/8.5/javadoc/org/gradle/api/tasks/testing/Test.html) type, and registers its default task `embulkPluginTest`. It would also define default [`Configuration`](https://docs.gradle.org/8.5/dsl/org.gradle.api.artifacts.Configuration.html)s and [`SourceSet`](https://docs.gradle.org/8.5/dsl/org.gradle.api.tasks.SourceSet.html)s. The `EmbulkPluginTest` task would configure the class loading structure with dependencies for testing.

The `build.gradle` would be like below.

```gradle
plugins {
    id "java"
    id "org.embulk.embulk-plugins" version "..."
}

// ...

dependencies {
    compileOnly "org.embulk:embulk-spi:..."

    implementation "org.embulk:embulk-util-config:..."
    // ...

    // Simple unit tests.
    testImplementation "org.junit.jupiter:junit-jupiter-api:..."
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:..."

    // "Integrated" Embulk plugin tests.
    embulkPluginTestImplementation "org.junit.jupiter:junit-jupiter-api:..."

    embulkPluginTestRuntimeOnly "org.embulk:embulk-junit5-engine:X.Y.Z"  // Embulk's custom Test Engine.
    embulkPluginTestRuntimeOnly "org.embulk:embulk-core:X.Y.Z"  // Embulk's core.
    embulkPluginTestDeps "org.embulk:embulk-deps:X.Y.Z"  // Embulk's core dependencies.
}

embulkPlugin {
    mainClass = "..."
    category = "..."
    type = "..."
}

test {
    // Configurations for the simple unit tests.
}

embulkPluginTest {
    // Configurations for the "integrated" Embulk plugin tests.
}
```

Alternatives considered
========================

JUnit 5 InvocationInterceptor
------------------------------

JUnit 5 has a mechanism to intercept test method invocations. This has been mentioned some times to tweak the class loading structure in JUnit 5.

* See: [Introduce extension API for customizing the ClassLoader in Jupiter: junit-team/junit5#3028](https://github.com/junit-team/junit5/issues/3028)

However, it would require a certain amount of tweaks in each plugin for plugin developers by themselves. It might work to test only the Embulk core, but it is not sufficient to provide it as a test framework for Embulk plugins.

JUnit 5 Launcher
-----------------

Another approach for the class loading structure was tweaking JUnit 5's "Launcher", but it was not a good fit.

* See: [JUnit Platform Launcher API from JUnit 5 User Guide (Version 5.10.1)](https://junit.org/junit5/docs/5.10.1/user-guide/#launcher-api)

The Launcher configuration is hard-coded in Gradle, indeed, when running tests from Gradle. Gradle does not provide a good configuration approach at least as of Gradle 8.5.

* See: [`JUnitPlatformTestClassProcessor`](https://github.com/gradle/gradle/blob/v8.5.0/platforms/jvm/testing-junit-platform/src/main/java/org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessor.java#L111-L122)

Gradle TestFramework
---------------------

Yet another approach for the class loading Gradle's `Test` could be configuring its "Test Framework", but it did not work as well.

Gradle's [`Test`](https://docs.gradle.org/8.5/javadoc/org/gradle/api/tasks/testing/Test.html) supports a couple of test frameworks including `useJUnit` (JUnit 4), `useTestNG` (TestNG), and `useJUnitPlatform` (JUnit 5). `useJUnitPlatform` has some configurations, but it does not work for the class loading structure.

[The `Test` class has a method `useTestFramework` to set an arbitrary `TestFramework` implementation](https://github.com/gradle/gradle/blob/v8.5.0/platforms/jvm/testing-jvm/src/main/java/org/gradle/api/tasks/testing/Test.java#L1083-L1100), but the method is package-private, which is only for Gradle-internal use. [The `TestFramework` interface](https://github.com/gradle/gradle/blob/v8.5.0/platforms/jvm/testing-jvm/src/main/java/org/gradle/api/internal/tasks/testing/TestFramework.java) is an internal API, too.

Furthermore, it would not work even the Gradle plugin hacks `TestFramework` somehow (ex. reflection). [`JUnitPlatformTestClassProcessorFactory`](https://github.com/gradle/gradle/blob/v8.5.0/platforms/jvm/testing-jvm-infrastructure/src/main/java/org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessorFactory.java) needs to be injected to tweak the class loading structure by replacing [`TestClassProcessor`](https://github.com/gradle/gradle/blob/v8.5.0/platforms/software/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/TestClassProcessor.java) ([`JUnitPlatformTestClassProcessor`](https://github.com/gradle/gradle/blob/v8.5.0/platforms/jvm/testing-junit-platform/src/main/java/org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessor.java) for ordinary JUnit 5) eventually. However, the `JUnitPlatformTestClassProcessorFactory` instance is once serialized, passed to another "Worker" Java VM process, deserialized there, and then used there. The injected `TestClassProcessor` class needs to be defined in the "Worker" process to be deserialized, but a Gradle plugin cannot step into Workers. No way. Checkmate.

Changes in the Embulk core
===========================

It would not change the Embulk SPI. It would not impact compatibility with Embulk plugins.

However, in order to inject the class loader from the testing framework, some changes are expected especially around `PluginClassLoader`.

Remaining issue
================

Even with the custom "Test Engine" approach, one issue still remains when running the "integrated" Embulk plugin tests from Gradle. Gradle loads the test classes into the test Worker Java VM before passing those classes to JUnit 5.

* See: [`JUnitPlatformTestClassProcessor`](https://github.com/gradle/gradle/blob/v8.5.0/platforms/jvm/testing-junit-platform/src/main/java/org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessor.java#L102-L109)

The custom Test Engine would load those classes again under `PluginClassLoader`. It shouldn't be a problem because `PluginClassLoader` is designed to prioritize its own loads, but the test classes on the top-level class loader may or may not cause an unexpected conflict.

It wouldn't be a problem if Gradle has an option to pass test classes to JUnit 5 only by class names, but Gradle does not have such an option at least as of Gradle v8.5. It would be feasible by [building `ClassSelector`s with class names](https://junit.org/junit5/docs/5.10.1/api/org.junit.platform.engine/org/junit/platform/engine/discovery/DiscoverySelectors.html#selectClass(java.lang.String)), not with class instances.

Copyright and License
======================

This document is placed under the CC0-1.0-Universal license, whichever is more permissive.
