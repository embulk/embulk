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

On the other hand, the existing `embulk-junit4` (`embulk-test`) has not been very easy to use, indeed. Building the brand new testing framework would be also a good chance for improvement.

This EEP explains the basic design of the new testing framework for Embulk plugins with JUnit 5, which is called `embulk-junit5`.

Goals
======

We aim the following points in `embulk-junit5` with improvements from `embulk-junit4`.

1. Being based on JUnit 5 (definitely)
2. Requiring no other Embulk plugins to test the target Embulk plugin
3. Reproducing a similar class loading structure to the real Embulk's class loading
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

It could definitely cause unintended results in testing. For example, while `embulk-deps` depends on `jackson-core:2.6.7`, the target plugin may depend on `jackson-core:2.15.3` and `jackson-datatype-guava:2.15.3`, and another depended plugin may depends on `jackson-core:2.9:10` and `jackson-datatype-guaga:2.9.10`. Nobody could tell how the test results would be!

Design
=======
