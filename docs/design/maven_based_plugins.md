Maven-based Plugins
====================

Plugins are loaded only through RubyGems except for embulk-standards and Guice modules until v0.8.25. Maven-based JAR plugins are supported since v0.8.26 so that users finally do not need JRuby.


Dependency Libraries
---------------------

A Maven-based plugin was expected to be a single JAR including its dependency libraries (fat JAR or JARs-in-JAR) until v0.9.7. It had had problems especially in publishing Maven-based plugins such as sizes of plugins, and licenses.

It was because resolving transitive dependencies is not deterministic in Maven. When a dependency library `A` depends on another library `B`, `B`'s version is not determined. Even worse, the transitive dependencies are resolved at Embulk runtime. It can easily result in difference between users. A plugin working at some users may not work at other users. It would really confuse ourselves.

The dependency problem is relaxed if all dependencies are guaranteed to be resolved at compile time. Embulk has started to find plugin's dependencies since v0.9.8, but the resolution is only from its "direct" dependencies to realize the guarantee.

To follow the restriction of direct dependencies, plugin developers need to resolve all nested dependencies at compile time, and to build `pom.xml` with all the transitive dependencies resolved.
