Release 0.3.2
==================================

General Changes
------------------

* Fixed a problem where ruby input plugins can't use timestamp type (reported
  by @shun0102)
* ``Embulk::Page`` includes Enumerable to include map, each_with_index, and other
   a lot of convenient methods (@niku++)
* Fixed ``spi.time.TimestampType::DEFAULT_FORMAT`` to use ':' as the separator of times

Release Date
------------------
2015-02-04
