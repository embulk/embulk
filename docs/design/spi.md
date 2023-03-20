Embulk SPI
===========

Embulk is going to have a separate `embulk-spi` artifact isolated from `embulk-core`, so that Embulk plugins will need to depend only on `embulk-spi`, not to have a direct dependency on `embulk-core`. Changes on the core side would impact plugins less easily. `embulk-spi` contains accessors for Embulk core features, and consists of interfaces and abstract classes for Embulk plugins to inherit.

Along with the isolation, "utility" classes in `embulk-core` are to be externalized as separate libraries. [`embulk-util-timestamp`](https://search.maven.org/artifact/org.embulk/embulk-util-timestamp) is an early example. Such utility classes are loaded in plugin's `ClassLoader`. Plugins would be responsible to choose utility libraries on their own. Utility libraries for Embulk plugins may also depend on `embulk-spi` to help plugins accessing Embulk core features.

`embulk-spi` is loaded in the same `ClassLoadder` with `embulk-core`. `embulk-core` depends on `embulk-spi`. The executable all-in-one binary contains `embulk-spi` extracted.
