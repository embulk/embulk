Release 0.6.20
==================================

Command line interface
------------------

* Added ``-X key=value`` argument to set system config. This argument is intended to overwrite performance parameters such as number of threads (``max_threads``) or page buffer size (``page_size``).


General Changes
------------------

* Change default size of page buffer from 8KB to 32KB.
* Size of a page buffer is configurable by system config (@sonots++). On command line, ``embulk`` command accepts ``-X page_size=N[unit]`` argument (e.g. ``-X page_size=512KB``).


Release Date
------------------
2015-08-03
