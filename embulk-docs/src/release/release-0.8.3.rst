Release 0.8.3
==================================

General Changes
------------------

* Fixed unexpected error when ``-c`` option is set and file doesn't exist yet (@hiroyuki-sato++)
* Added a workaround for https://github.com/jruby/jruby/issues/3652
* Adds thread ID to thread name so that stacktrace shown by SIGQUIT can distinguish multiple threads running the same stage (transaction, guess, preview).
* Embulk::Guess::SchemaGuess returns ``"json"`` type if a given value is a Hash or Array.


Release Date
------------------
2016-02-08
