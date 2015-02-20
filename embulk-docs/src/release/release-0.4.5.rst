Release 0.4.5
==================================

CLI
------------------

* Fixed a problem where ``embulk gem install foo --version x.y.z`` shows embulk's version.

Plugin SPI
------------------

* Added ParserPlugin SPI for JRuby. You can write parser plugins in Ruby.
* Added FormatterPlugin SPI for JRuby. You can write formatter plugins in Ruby.


Plugin API Changes
------------------

* Added ``Embulk::FileInput`` API used mainly by parser plugins.
* Added ``Embulk::FileOutput`` API used mainly by formatter plugins.

Release Date
------------------
2015-02-19
