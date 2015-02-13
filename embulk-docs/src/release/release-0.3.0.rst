Release 0.3.0
==================================

Resume Feature
------------------

* Added resume functionality.

Filter Feature
------------------

* Added new filter plugin type.

Plugin SPI
------------------

* ``spi.InputPlugin`` and ``spi.OutputPlugin`` need to implement ``resume`` and ``cleanup`` methods.
* Added ``spi.FilterInputPlugin`` Java API.
* Added ``Embulk::FilterInputPlugin`` JRuby API.

CLI
------------------

* ``run`` subcommand supports ``-r, --resume-state PATH`` option.

General Changes
------------------

* Added gradle-versions-plugin to build.gradle (@seratch++)
* Fixed broken dependencies at build.gradle (@thagikura++)

Release Date
------------------
2015-02-03
