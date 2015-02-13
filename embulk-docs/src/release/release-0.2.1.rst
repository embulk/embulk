Release 0.2.1
==================================

Plugin Utility
------------------

* Fixed ``spi.util.LineEncoder#finish`` to flush all remaining buffer (reported by @aibou)
* ``Embulk::PageBuilder#add`` accepts nil value

General Changes
------------------

* Fixed NextConfig to be merged to in: or out: rather than the top-level
  (reported by enukane) [#41]
* ./bin/embulk shows warns to run `rake` if ./classpath doesn't exist

Release Date
------------------
2015-01-29
