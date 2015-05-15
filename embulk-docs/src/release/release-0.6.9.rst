Release 0.6.9
==================================

Built-in plugins
------------------

* ``formatter-csv`` supports ``quote``, ``quote_policy``, ``escape``, ``newline_in_field``, and ``null_string`` options (@sakama++)

  * ``quote_policy`` controls how to quote values. It can be either of ``ALL`` (quote all values), ``MINIMAL`` (quote if a value includes delimiter or quote character), or ``NONE`` (never quotes).

  * ``escape`` controls how to escape quote character in a quoted string. The default is ``"`` (``"`` will be ``""``). Some applications may set it to ``\`` (``"`` will be ``\"``)

  * ``null_string`` controls how to write NULL values. The default is ``""`` (empty string). You can use any strings such as ``\N`` or ``#N/A``.

* ``guess-csv`` guesses columns which contain only 0 and 1 in first 32KB as long type rather than boolean type.

General Changes
------------------

* ``spi.util.LineEncoder`` uses buffered writer. This improves performance of ``formatter-csv`` upto 10%.

Release Date
------------------
2015-05-14
