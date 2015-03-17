Release 0.5.3
==================================

Built-in plugins
------------------

* ``guess`` guesses boolean types (@hata++)


General Changes
------------------

* ``guess`` stops reading the file after first 32KB and ignores remaining data. Execution time of ``guess`` improves significantly especially when the first file is large.

* ``guess`` ignores decoders when it reads sample data. This change fixes the problem where guess fails if config file includes ``decoder-gzip`` (@hata++).

* Releases ``embulk-core-VERSION-tests.jar`` in addition to ``embulk-core-VERSION-sources.jar``.


Release Date
------------------
2015-03-17
