Embulk API and SPI
===================

Embulk is going to have separate `embulk-api` and `embulk-spi` artifacts isolated from `embulk-core`, so that Embulk plugins will need to depend only on them, not to have a direct dependency on `embulk-core`. Changes on the core side would impact plugins less easily.

Along with the isolation, "utility" classes in `embulk-core` are to be externalized as separate libraries. [`embulk-util-timestamp`](https://search.maven.org/artifact/org.embulk/embulk-util-timestamp) is an early example. Such utility classes are loaded in plugin's `ClassLoader`. Plugins would be responsible to choose utility libraries on their own.

* `embulk-api` contains accessors for Embulk core features. Utility libraries for Embulk plugins may also depend on `embulk-api` to help plugins accessing Embulk core features.
* `embulk-spi` consists of interfaces and abstract classes for Embulk plugins to inherit.

`embulk-api` and `embulk-spi` are loaded in the same `ClassLoadder` with `embulk-core`. `embulk-core` depends on `embulk-api` and `embulk-spi`. The executable all-in-one binary contains both `embulk-api` and `embulk-spi` extracted.
