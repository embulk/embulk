Release 0.6.17
==================================

General Changes
------------------

* '-' in generated plugin name is replaed to '_'.

* Package name of generated java plugins changed from org.embulk.<category> to org.embulk.<category>.<plugin name>.

* Generated plugin templates use recommended utility classes by default.

  * java-decode template generator uses InputStreamFileInput.

  * java-encoder template generator uses FileOutputOutputStream.

  * java-file-input template generator uses InputStreamTransactionalFileInput.

  * guess method of generated java-input plugin returns empty config diff rather throwing an exception.


Java Plugin API
------------------

* Added ``spi.Exec.getTransactionTime()``.
* Added ``spi.util.Timestamps`` utility class. This method is useful to create ``TimestampParser`` and ``TimestampFormatter`` which are configurable by users.
* Added ``spi.util.InputStreamFileInput.Opener`` interface to open single file.
* Added ``spi.util.InputStreamFileInput()`` with ``InputStream`` to use a pre-opend stream.
* Added ``spi.util.InputStreamTransactionalFileInput`` for convenience of ``FileInputPlugin``.

Ruby Plugin API
------------------

* Added ``FileInput#to_java`` and ``FileOutput#to_java``.


Release Date
------------------
2015-07-17
