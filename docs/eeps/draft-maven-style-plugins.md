---
EEP: unnumbered
Title: Maven-style Plugins
Author: dmikurube
Status: Draft
Type: Standards Track
Created: 2023-06-19
---

Introduction
=============

This EEP explains how Embulk's new Maven-style plugin mechanism is designed. Maven-style plugins are designed to overcome the difficulties of Embulk's old RubyGems-style plugins.

Motivation
===========

Embulk's plugins have been distributed as RubyGems, even if they are coded in Java. The RubyGems-style plugins, however, have had several drawbacks.

* RubyGems-style plugins require JRuby.
    * JRuby need a lot of resources, such as CPU time, Java heap memory, and metaspace.
    * JRuby takes a long time to initialize itself.
    * Making JRuby optional is another effort to be explained in another EEP. This EEP would compose a part of the effort.
* Bundler is mandatory to select a specific version of the same RubyGems-style plugin if multiple versions are installed.
    * Users sometimes wanted to select a specific version from the same installation set.
    * Reconfiguring Bunlder needs time, and sometimes reinstallation.
* A RubyGems-style plugin has to include all the dependency JAR files packaged.
    * The size of a RubyGems-style plugin package tended to be large.
    * Many Embulk plugins are expected to depend on the same Java libraries. They would be duplicated on the network for downloading, and on the file system.
    * License matters are more complicated in a RubyGems-style plugin because it includes redistributions.

These are unavoidable as long as the plugins are distributed as RubyGems. Another style of Embulk plugins is needed to resolve them.

Maven-style Plugins
====================

Requirements
-------------

Consequently, the new style of Embulk plugins would need the following characteristics to overcome the drawbacks of the RubyGems style above.

1. The new plugin style must not require JRuby, nor any other heavy tooling.
2. The new plugin style must accept multi-version installations of the same plugin, and a user can select a specific version at runtime from the installations.
3. The new plugin style must include a mechanism to resolve dependencies at the timing of installation, or runtime.

Maven
------

To cover the requirements #1 and #3 above, [Apache Maven](https://maven.apache.org/) is effectively the only option. [Maven Central](https://search.maven.org/) is the de-facto standard in Java open-source package registries. Almost all the Embulk plugins would depend on open-source Java libraries distributed at Maven Central.

A new-style Embulk plugin would depend on Maven-based library distributions. Then, it would be natural to distribute the Embulk plugin also as a Maven artifact. The requirement #2 could be achieved by distributing an Embulk plugin as a Maven artifact.

One more point: Maven Central would also be the first option for publishing open-source Embulk plugins. We've seen [the sunset of JFrog's JCenter and Bintray](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/). Needless to say, in-house Embulk plugins can be managed in in-house Maven package registries, such as [JFrog Artifactory](https://jfrog.com/artifactory/) and else.

The new plugin style would be called "Maven-style plugins".

Just in case, a single JAR file including its dependency libraries (so-called "fat JAR") is not our choice. Fat JARs do not satisfy the requirement #3.

Tooling in Embulk
------------------

Although access to Maven Central is mandatory, there are still some options to access Maven. There is no limitation for the access. The Apache Maven `mvn` command could be used, or [Gradle](https://gradle.org/) could be another option, for example.

However, we wanted to keep the Embulk core "compact", as discussed in [EEP-3](./eep-0003.md). It would be a reasonable option to embed a minimum part of Apache Maven as a library in the Embulk core. We may also use [Aether](https://wiki.eclipse.org/Aether/What_Is_Aether), a convenient extension library to Apache Maven. [Aether is imported to Apache Maven](https://maven.apache.org/aether.html) now.

Note that it does not mean any other possibility is limited. There is room to implement an external tooling out of the Embulk core. For instance, a standalone command, a Maven extension with the `mvn` command, or a use of Gradle with an extension plugin would be possible options.

No on-demand installation
--------------------------

For reference, [Digdag](https://www.digdag.io/) supports a similar Maven-based extension. It supports on-demand installation that downloads and installs Maven artifacts at runtime from the runtime configurations.

However, we do not adopt this approach in Embulk. The download/installation and the use are separated intentionally. The reasons follow.

* The major use of Embulk is on servers. When a user deploys Embulk with plugins onto a server, they usually have a fixed deployment package and expect the deployed package to be used as-is.
* On-demand installation could be an unexpected attack surface, especially on a server. Only intended plugin versions should run there.
* A plugin sometimes has intended or unintended incompatibility between its versions. Enabling on-demand installation would have a risk of accidental upgrade (or downgrade), which could result in an unexpected incident.

Dependencies are resolved at the installation timing, not at runtime, as a result.

Intended limitation for stable dependency resolution
-----------------------------------------------------

Dependency resolution can sometimes be "unstable" or non-deterministic, unfortunately. It happens especially for transitive dependencies. When a dependency library `A` depends on another library `B`, `B`'s version could be different. Even Maven and Gradle take different approaches to dependency resolution.

> Maven dependency conflict resolution works with a shortest path, which is impacted by declaration ordering. Gradle does full conflict resolution, selecting the highest version of a dependency found in the graph. In addition, with Gradle you can declare versions as strictly which allows them to take precedence over transitive versions, allowing to downgrade a dependency.
>
> (from [Gradle vs Maven Comparison](https://gradle.org/maven-vs-gradle/) at [gradle.org](https://gradle.org/))

If such a non-deterministic behavior happened at runtime on a server, a catastrophe could potentially occur in "production" environments.

To avoid such an incident, Embulk introduces an intentional limitation in its Maven dependency resolution. Embulk loads only the "direct" dependencies of an Embulk plugin artifact. In other words, Embulk loads only dependencies declared explicitly in `<dependencies>` of the plugin's `pom.xml`.

It means all dependencies must be resolved when a plugin developer builds and releases an Embulk plugin. It may bother some plugin developers, but users would benefit from stable dependency resolution.

On the other hand, each Embulk plugin would be nice to have `<exclusions>` for everything in its `pom.xml` to be consistent with other Maven toolings. See the following example.

```
<dependencies>
  ...
  <dependency>
    <groupId>org.embulk</groupId>
    <artifactId>embulk-util-config</artifactId>
    <version>0.3.4</version>
    <scope>compile</scope>
    <exclusions>
      <exclusion>
        <groupId>*</groupId>
      </exclusion>
    </exclusions>
  </dependency>
  ...
</dependencies>
```

A helper tooling could help plugin developers to build a compliant Embulk plugin.

Plugin installation directory
------------------------------

Maven-style plugins must be installed on the local file system in some form. For extensibility with other third-party toolings, Embulk adopts and expects [the standard Maven local repository](https://maven.apache.org/repositories/local.html) format in [the standard Maven layout](https://maven.apache.org/repositories/layout.html). The layout is the same as the well-known `~/.m2/repository/`.

The location of the installation directory is specified by the Embulk System Properties and the Embulk Home directory designed and explained in [EEP-4](./eep-0004.md).

User configurations
--------------------

The existing YAML configuration format does not work for users to select a specific version at runtime.

```
in:
  type: s3
  bucket: ...
  path_prefix: ...
  ...
```

The format needs to be extended to specify a version of the plugin. In addition, a Maven artifact needs to configure its `groupId`, in addition to its `artifactId`. Embulk provides two ways to configure with Maven-style plugins.

### Inline configuration for Maven-style plugins

Users can configure the YAML inline for Maven-style plugins under `type` expanded to `source`, `group`, `name`, and `version`. The example below configures to run with the S3 Input Plugin of the Maven-style plugin artifact `org.embulk:embulk-input-s3:0.5.3`.

```
in:
  type:
    source: maven
    group: org.embulk
    name: s3
    version: 0.5.3
  bucket: ...
  path_prefix: ...
```

Note that the `artifactId` is not fully specified like `embulk-input-s3` there. `name` is the same as the `type` of an existing RubyGems-style plugin. The `artifactId` is complemented automaticaly by adding the prefix `embulk-` and the category of the plugin (`input-`).

### Configuration for Maven-style plugins through Embulk System Properties

The other way to configure with Maven-style plugins is Embulk System Properties. See [EEP-4](./eep-0004.md) for Embulk System Properties.

Once a user sets an Embulk System Property `plugins.<category>.<type>` in the format of `maven:<group>:<name>:<version>`, the same YAML configuration as RubyGems-style keeps working. The following examples configure the same with the above, to run with the S3 Input Plugin of the Maven-style plugin artifact `org.embulk:embulk-input-s3:0.5.3`.

```
# embulk.properties
plugins.input.s3=maven:org.embulk:s3:0.5.3
```

```
# s3.yaml
in:
  type: s3
  bucket: ...
  path_prefix: ...
```

The latter with Embulk System Properties is expected to be less confusing for users.

Helpers
========

Helper to build and release an Embulk plugin
---------------------------------------------

Building and releasing a compliant Embulk plugin with resolved dependencies is not trivial for plugin developers. Plugin developers would feel bored doing such a thing by themselves.

We need to provide kinds of helpers for plugin developers. Java-based Embulk plugins have been built with Gradle. Providing a Gradle plugin to build and release a compliant Embulk plugin is one of the reasonable approaches.

[The `org.embulk.embulk-plugins` Gradle plugin](https://plugins.gradle.org/plugin/org.embulk.embulk-plugins) is the ready-made Gradle plugin for that by the Embulk maintainer team. Note that it does not restrict any other helper tooling from being developed.

Helper to download and install Embulk plugins
----------------------------------------------

Many users have managed their RubyGems-style plugin installation sets with Bundler. They would want similar tooling for Maven-style plugins.

There can be various possibilities for such toolings. One possibility is another Gradle plugin to download and install Maven-style plugins into the configured plugin installation directory. Such a tooling may also configure Embulk System Properties for specific Maven-style plugin versions.

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
