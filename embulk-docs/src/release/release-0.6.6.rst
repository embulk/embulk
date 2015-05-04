Release 0.6.6
==================================

General Changes
------------------

* Fixed a problem that timestamp format guess code can't guess format if the string includes day of 30 and 31.
* ``guess`` and ``preview`` throw NoSampleException with appropriate error message if there are no input tasks.

Plugin API
------------------

* ``spi.util.InputStreamFileInput#close`` closes currently opened InputStream.

Release Date
------------------
2015-05-04
