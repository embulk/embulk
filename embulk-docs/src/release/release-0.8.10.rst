Release 0.8.10
==================================

General Changes
------------------

* Fixed 'IllegalArgumentException: Self-suppression not permitted' error (@hata++) [#446]

* Fixed preview not to read the entire file when the parser doesn't produce records. Now preview reads first 32KB.

* Updated JRuby from 9.0.4.0 to 9.1.2.0. Release notes:

  * http://jruby.org/2016/01/26/jruby-9-0-5-0.html

  * http://jruby.org/2016/05/03/jruby-9-1-0-0.html

  * http://jruby.org/2016/05/19/jruby-9-1-1-0.html

  * http://jruby.org/2016/05/27/jruby-9-1-2-0.html

* Updated msgpack-java from 0.8.7 to 0.8.8. Release notes

  * https://github.com/msgpack/msgpack-java/blob/0.8.8/RELEASE_NOTES.md

Built-in plugins
------------------

* ``csv`` parser plugin supports delimiters longer than 1 character.

* ``csv`` parser doesn't convert non-quoted empty string into NULL any more when null_string is set. Default behavior is not changed (convert non-quoted empty string into NULL).


Release Date
------------------
2016-07-21
