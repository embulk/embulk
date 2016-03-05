Release 0.8.7
==================================

General Changes
------------------

* LineDecoder skips UTF-8 BOM appropriately.

* Timestamp guess uses ``%z`` instead of ``%Z``. Parsing behavior doesn't change but formatting behavior changes if output plugins use guessed configuration to format timestamp (this is unlikely because guessed configuration is most likely used by input plugins to parse timestamp).

* Fix uninitialized constant MessagePack (@sonots++)

* Fixed a possible busy loop at TaskDeserializer.


Release Date
------------------
2016-03-04
