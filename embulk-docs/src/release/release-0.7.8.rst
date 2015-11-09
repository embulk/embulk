Release 0.7.8
==================================

General Changes
------------------

* Added "mkbundle" subcommand. Its help messae shows how to use plugin blundes with examples.
* When a transaction throws an exception and cleanup code (close() method) also throws an exception, the exception of transaction was hidden. Now the exception of transaction is the primary error and exception of cleanup code is also included as a suppressed exception.
* CSV parser plugin doesn't show lines twice with "Unexpected extra character" error.


Release Date
------------------
2015-11-08
