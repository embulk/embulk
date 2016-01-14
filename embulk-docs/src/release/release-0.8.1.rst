Release 0.8.1
==================================

General Changes
------------------

* Ruby-based plugins use ``>=`` instead of ``~>`` to depend on bundler and embulk so that users can use both older plugins and newer plugins together. This assumes major versions of those dependencies most likely don't break backward compatibility.

* Added bulit-in dependency to msgpack.gem v0.7.4 which is used to support json types at ruby plugins.

* Fixed json value creation using ruby-based parser and input plugins.

* Fixed preview of json types.


Release Date
------------------
2016-01-13
