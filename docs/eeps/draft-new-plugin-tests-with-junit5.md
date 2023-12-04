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

Requirements
=============

With improvements from `embulk-junit4`, the new testing framework would need the following points.

1. Being based on JUnit 5 (definitely)
2. Reproducing a similar class loading structure to the real Embulk's class loading
3. Requiring no other Embulk plugins to test the target Embulk plugin
4. ...
