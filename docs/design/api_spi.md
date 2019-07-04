Embulk API and SPI
===================

Embulk is going to have `embulk-api` and `embulk-spi` artifacts isolated from `embulk-core`, so that plugins will not have a direct dependency on `embulk-core`, and plugins are less easily affected by changes on the core side.

Along with that isolation, types of "utility" features in `embulk-core` are going to be externalized as separate libraries. [`embulk-util-timestamp`](https://search.maven.org/artifact/org.embulk/embulk-util-timestamp) is an example, and to be more.
