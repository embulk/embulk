---
EEP: unnumbered
Title: Milestones to Embulk v1.0, and versioning strategies to follow
Author: dmikurube
Status: Draft
Type: Standards Track
---

Motivation
===========

The Embulk core has completed the huge architectural transition through the Embulk v0.10 "development" series.

* [EEP-4: System-wide Configuration by Embulk System Properties and Embulk Home](./eep-0004.md)
* [EEP-5: Maven-style Plugins](./eep-0005.md)
* [EEP-6: JRuby as Optional](./eep-0006.md)
* [EEP-7: Core Library Dependencies](./eep-0007.md)

The stable production versions, v1.0, have come into our field of vision, but we are not clear that when we declare v1.0.

The goal of this EEP is to clarify the milestones for Embulk v1.0, and to suggest future versioning strategies to follow v1.0.

Milestones to v1.0
===================

The Embulk core v1.0 would be released when the following criteria are satisfied.

1. The Embulk core introduces a sub-command to install Maven-style plugins.
2. A tooling out of the Embulk core package to install Maven-style plugins is released.
3. The new testing framework for Embulk plugins is ready with JUnit 5.
4. Dependency libraries of the Embulk core are up-to-date.
5. Dependency libraries of the built-in plugins are up-to-date.
6. The Embulk core is tested to work on Java 11 and/or 17 with some major plugins.

The core and SPI versions through v0.11 - v1.1
-----------------------------------------------

The Embulk core and the Embulk SPI have different version strategies. Around v1.0, they would have the following versions.

All those versions would work at least with Java 8.

| Embulk core version | Embulk SPI version                                      | Required Java version |
| ------------------- | ------------------------------------------------------- | --------------------- |
| `v0.11.*`           | `v0.11`                                                 | `8`                   |
| `v1.0.*`            | `v0.11` (or `v0.12` with a few small compatible fixes)  | `8` or later          |
| `v1.1.*`            | `v1.0` (with removing `msgpack-core` from `embulk-spi`) | `8` or later          |

Versioning strategies to follow v1.0
=====================================

Embulk SPI
------------

The Embulk SPI has two-digit version numbers such as `v1.2`.

Embulk core
------------

The Embulk core has three-digit version numbers such as `v1.2.3`. The versioning strategies after Embulk v1.1 would give some respects to the [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) while it may not be very strict.

The MAJOR version (the first digit of the version number) is incremented when the Embulk core has an incompatible change. The "incompatible change" includes its required Java version. In other words, the Embulk core version would be v2.*.* or further when Embulk requires Java 11, 17, or 21.

The MINOR version (the second digit of the version number) is incremented when the Embulk core has something new which is compatible, but not just a fix.

The PATCH version (the third digit of the version number) is incremented when the Embulk core has a fix.

Copyright and License
======================

This document is placed under the CC0-1.0-Universal license, whichever is more permissive.
