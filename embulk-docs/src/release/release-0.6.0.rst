Release 0.6.0
==================================

Executor Plugin Mechanism
------------------

Now executor of Embulk is fully extensible using plugins. Executor plugins get input, filter and output plugins from the Embulk framework and runs them using multiple threads, processes, or servers. While input, filter and output plugins are response for data processing, executor plugins are responsible for scheduling the processing tasks and managing parallelism for performance.

The built-in executor plugin is ``LocalExecutorPlugin`` that runs tasks using multiple threads. It has a shared thread pool and schedules tasks at most ``(number of available CPU cores) * 2`` tasks in parallel. Number of threads is configurable using ``max_threads`` system parameter.

Another available executor is `embulk-executor-mapreduce <https://github.com/embulk/embulk-executor-mapreduce>`_ plugin. This executor plugin runs tasks on Hadoop, a distributed computing environment. It is suitable for processing TBs of data. An unique functionality is that it supports partitioning data by a certain column before passing them to output plugins. An example use case is that the MapReduce executor partitions data by time so that files on destination storage are partitioned for each day.

Plugin API
------------------

* ``exec.LocalExecutor`` class is separated into ``exec.BulkLoader`` class for interface definition and ``exec.LocalExecutorPlugin`` for implementation. If you're application is Embulk through ``LocalExecutor`` class, you need to replace it with ``BulkLoader``.
* ``spi.ExecAction#run`` can throw ``Exception`` and ``Exec.doWith(ExecSession, ExecAction<T>)`` throws ``ExecutionException``.
* ``spi.Buffer`` implements ``#equals`` and ``hashCode`` methods.

Plugin SPI
------------------

* Added ``spi.ExecutorPlugin`` interface.
* Added ``Embulk::ExecutorPlugin`` class.

General Changes
------------------

* If there are no input tasks, the transaction is committed successfully rather than making it failed.


Release Date
------------------
2015-04-07
