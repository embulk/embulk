.. Embulk documentation master file, created by
   sphinx-quickstart on Fri Feb 13 14:52:36 2015.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Embulk
==================================

.. image:: _static/embulk-logo.png
   :width: 512px
   :target: https://github.com/embulk/embulk

What's Embulk?
------------------

Embulk is a open-source bulk data loader that helps data transfer between various databases, storages, file formats, and cloud services.

Embulk supports:

* Automatic guessing of input file formats
* Parallel & distributed execution to deal with big data sets
* Transaction control to guarantee All-or-Nothing
* Resuming
* Plugins released on RubyGems.org

You can define a bulk data loading using combination of input and output plugins:

.. image:: _static/embulk-architecture.png
   :width: 640px
   :target: https://github.com/embulk/embulk#quick-start

For example, `this tutorial <recipe/scheduled-csv-load-to-elasticsearch-kibana4.html>`_ describes how to use **file** input plugin with **csv** parser plugin and **gzip** decoder plugin to read CSV files, and **elasticsearch** output plugin to load the records to Elasticsearch.

Documents
------------------

* `Quick Start <https://github.com/embulk/embulk#quick-start>`_

  * `Linux <https://github.com/embulk/embulk#linux--mac--bsd>`_

  * `Mac OS X <https://github.com/embulk/embulk#linux--mac--bsd>`_

  * `Windows <https://github.com/embulk/embulk#windows>`_

.. toctree::
   :maxdepth: 2

   recipe

* `List of Plugins by Category <http://www.embulk.org/plugins/>`_

  * `Input plugins <http://www.embulk.org/plugins/#input>`_

  * `Output plugins <http://www.embulk.org/plugins/#output>`_

  * `File parser plugins <http://www.embulk.org/plugins/#file-parser>`_

  * `File decoder plugins <http://www.embulk.org/plugins/#file-decoder>`_

  * `File formatter plugins <http://www.embulk.org/plugins/#file-formatter>`_

  * `Filter plugins <http://www.embulk.org/plugins/#filter>`_

  * `File encoder plugins <http://www.embulk.org/plugins/#file-encoder>`_

  * `Executor plugins <http://www.embulk.org/plugins/#executor>`_

.. toctree::
   :maxdepth: 3

   built-in

.. toctree::
   :maxdepth: 3

   customization

* `JavaDoc <javadoc/index.html>`_

* `RDoc <rdoc/_index.html>`_

* `Github <https://github.com/embulk/embulk>`_

.. toctree::
   :maxdepth: 2

   release

