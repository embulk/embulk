Release 0.4.6
==================================

CLI
------------------

* Updated installation script to install ``embulk`` binary to ~/.embulk/bin directory.
* Updated boot script to set ``-XX:+TieredCompilation`` and ``-XX:TieredStopAtLevel=1`` JVM options excepting ``run`` mode.
  * Those change improves startup time of embulk command 1.5 ~ 2x faster.
* Updated boot script to set ``-XX:+UseConcMarkSweepGC`` JVM option to ``run`` mode.
* Usage message shows embulk version.

Built-in plugins
------------------

* ``input/file`` plugin sets ``last_path`` option to the next configuration.
  * This enables scheduled execution where next execution loads files created after the last execution.
* ``guess/csv`` fixed guess result when the ordr of dates is not year, month day.
* ``guess/csv`` guesses dd/mm/yyyy time format if the input data includes at least one day larger than 12.
* ``guess/csv`` guesses RFC 2822 date and time formats
* ``guess/csv`` supports "." as the delimiter of year, month or day.

General Changes
------------------

* embulk command raises a special message if HOME environment variable is not set.

Release Date
------------------
2015-02-20
