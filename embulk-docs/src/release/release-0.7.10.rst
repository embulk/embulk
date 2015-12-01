Release 0.7.10
==================================

General Changes
------------------

* Fixed a problem where guessing reads only 512 bytes when input is gzip-compressed and gzip decoder is guseed during the guessing.
* Added ``PartialExecutionException.getTransactionStage()`` method that tells in which stage a transaction failed. It will be either of beginning of (input, filter, executor, output), run, end of (output, executor, filter, input), or cleanup.


Release Date
------------------
2015-12-01
