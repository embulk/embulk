Release 0.6.2
==================================

Built-in plugins
------------------

* ``guess-csv`` guesses charset as ``"MS932"`` instead of ``"Shift_JIS"`` because practically almost all of documents encoded by ``Shift_JIS`` are created by Windows and ``Shift_JIS`` implemented by Microsoft means ``MS932`` in Java. (@kosaki55tea++, @nalsh++)
* ``parser-csv`` recovers errors by invalid number formats and skips the row rather than making entire transaction failed (@hito4t++)

General Changes
------------------

* Changed download URL. We can use the consitent URL to download the latest jar.

Release Date
------------------
2015-04-13
