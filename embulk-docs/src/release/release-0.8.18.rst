Release 0.8.18
==================================

General Changes
------------------

* Fixed schema guess plugin and changed a condition to guess double type. [#554]

* Fixed guess plugin that doesn't not use last line in sample_lines for schema guess. [#551]

* Fixed Json parser plugin that enables to parse non numeric numbers as floating number values. [#548]

* Updated msgpack-ruby v1.1.0 (@hiroyuki-sato) [#539, #553]

  * ChangeLog: https://github.com/msgpack/msgpack-ruby/blob/master/ChangeLog

* Downgraded netty-buffer 4.0.x for Embulk instead of 5.x. [#549]

* Minor fix:

  * Fixed typos in v0.7.8 release note. (@yoshihara) [#552]

  * Updated download link to be https. (@cosmok) [#544]

Release Date
------------------
2017-03-06
