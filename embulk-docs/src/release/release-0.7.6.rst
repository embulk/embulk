Release 0.7.6
==================================

General Changes
------------------

* On Cygwin/Windows environment, embulk command returns exit code 1 correctly if a transaction fails (@hishidama++)


Built-in plugins
------------------

* ``formatter-csv``: fixed a problem where a ``"`` character is escaped when ``quote_plicy: NONE`` is set. The fixed behavior is that, if ``quote_plicy: NONE`` is set, ``quote`` option (which means ``"`` character) is completely ignored, and only newline and delimiter are escaped ([#327] @sonots++).


Release Date
------------------
2015-10-27
