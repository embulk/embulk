Release 0.5.4
==================================

Built-in plugins
------------------

* ``parser-csv`` supports ``allow_optional_columns`` option. With this option set to ``true``, the parser sets null to insufficient columns rather than skipping the entire row (@kamatama41++)

* Fixed exception handling of ``parser-csv`` so that the transaction properly fails with underlaying exceptions such as IOException


General Changes
------------------

* Increased buffer size from 256 bytes to 32 KB. This improves performance significantly. (@hito4t++)

* If plugin type is null, suggest to use ``{type: "null"}`` (@hito4t++)

* Embulk logo is available! See the orca: https://github.com/embulk/embulk/issues/12


Release Date
------------------
2015-03-23
