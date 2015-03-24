.. Embulk documentation master file, created by
   sphinx-quickstart on Fri Feb 13 14:52:36 2015.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Embulk
==================================

.. image:: _static/embulk-logo.png
   :width: 512px

Embulk is a open-source bulk data loader that helps data transfer between various databases, storages, file formats, and cloud services.

Embulk supports:

* Automatic guessing of file formats
* Resuming
* Parallel & distributed execution to deal with big data sets
* Transaction control to guarantee All-or-Nothing
* Plugins released on RubyGems.org

You can define a bulk data flow by combination of input and output plugins. For example, `this tutorial <recipe/scheduled-csv-load-to-elasticsearch-kibana4.html>`_ shows how to use **file** input plugin with **csv** parser plugin and **gzip** decoder plugin to read files, and **elasticsearch** output plugin to load the records to Elasticsearch.

* `Quick Start <https://github.com/embulk/embulk#quick-start>`_

  * `Linux and Mac OS X <https://github.com/embulk/embulk#linux--mac--bsd>`_

  * `Windows <https://github.com/embulk/embulk#windows>`_


* `List of Plugins by Category <http://www.embulk.org/plugins/>`_

  * `Input plugins <http://www.embulk.org/plugins/#input>`_

  * `Output plugins <http://www.embulk.org/plugins/#output>`_

  * `File parser plugins <http://www.embulk.org/plugins/#file-parser>`_

  * `File formatter plugins <http://www.embulk.org/plugins/#file-formatter>`_

  * `Filter plugins <http://www.embulk.org/plugins/#filter>`_

.. toctree::
   :maxdepth: 2

   recipe
   release


* `JavaDoc <javadoc/index.html>`_

* `RDoc <rdoc/_index.html>`_

* `Github <https://github.com/embulk/embulk>`_

