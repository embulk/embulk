Scheduled bulk data loading to Elasticsearch + Kibana 4 from CSV files
==================================

.. contents::
   :local:
   :depth: 2

This article shows how to:

* Bulk load CSV files to Elasticsearch.
* Visualize the data with Kibana interactively.
* Schedule the data loading every hour using cron.

This guide assumes you are using Ubuntu 12.0 Precise.

Setup Elasticsearch and Kibana 4
------------------

Step 1. Download and start Elasticsearch.
~~~~~~~~~~~~~~~~~~

You can find releases from the `Elasticsearch website <http://www.elasticsearch.org/download/>`_.
For the smallest setup, you can unzip the package and run `./bin/elasticsearch` command:

.. code-block:: console

    $ ls
    $ wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.4.zip
    $ unzip elasticsearch-1.4.4.zip
    $ cd elasticsearch-1.4.4
    $ ./bin/elasticsearch

Step 2. Download and unzip Kibana:
~~~~~~~~~~~~~~~~~~

You can find releases from the `Kibana website <http://www.elasticsearch.org/overview/kibana/installation/>`_. Open a new console and run following commands:

.. code-block:: console

    $ wget https://download.elasticsearch.org/kibana/kibana/kibana-4.0.0-linux-x64.tar.gz
    $ tar zxvf kibana-4.0.0-linux-x64.tar.gz
    $ ./bin/kibana

Now Elasticsearch and Kibana started. Open http://localhost:5601/ using your browser to see the Kibana's graphical interface.


Setup Embulk
------------------

Step 1. Download Embulk binary:
~~~~~~~~~~~~~~~~~~

You can find the latest embulk binary from the `releases <https://bintray.com/embulk/maven/embulk/view#files>`_. Because Embulk is a single executable binary, you can simply download it to /usr/local/bin directory and set executable flag as following:

.. code-block:: console

    $ sudo wget https://bintray.com/artifact/download/embulk/maven/embulk-0.4.5.jar -O /usr/local/bin/embulk
    $ sudo chmod +x /usr/local/bin/embulk

Step 2. Install Elasticsearch plugin
~~~~~~~~~~~~~~~~~~

You also need Elasticsearch plugin for Embulk. You can install the plugin with this command:

.. code-block:: console

    $ embulk gem install embulk-output-elasticsearch

Embulk includes CSV file reader in itself. Now everything is ready to use.

Loading a CSV file
------------------

Assuming you have a CSV files at ``./mydata/csv/`` directory. If you don't have CSV files, you can create ones using ``embulk example ./mydata`` command.

Create this configuration file and save as ``config.yml``:

.. code-block:: yaml

    in:
      type: file
      path_prefix: ./mydata/csv/
    out:
      type: elasticsearch
      index_name: embulk
      index_type: embulk
      nodes:
        - host: localhost

In fact, this configuration lacks some important information. However, embulk guesses the other information. So, next step is to order embulk to guess them:

.. code-block:: console

    $ embulk guess config.yml -o config-complete.yml

The generated config-complete.yml file should include complete information as following:

.. code-block:: yaml

    in:
      type: file
      path_prefix: ./mydata/csv/
      decoders:
      - {type: gzip}
      parser:
        charset: UTF-8
        newline: CRLF
        type: csv
        delimiter: ','
        quote: '"'
        escape: ''
        null_string: 'NULL'
        header_line: true
        columns:
        - {name: id, type: long}
        - {name: account, type: long}
        - {name: time, type: timestamp, format: '%Y-%m-%d %H:%M:%S'}
        - {name: purchase, type: timestamp, format: '%Y%m%d'}
        - {name: comment, type: string}
    out:
      type: elasticsearch
      index_name: embulk
      index_type: embulk
      nodes:
      - {host: localhost}

Now, you can run the bulk loading:

.. code-block:: console

    $ embulk run config-complete.yml -o next-config.yml

Scheduling loading by cron
------------------

At the last step, you ran embulk command with ``-o next-config.yml`` file. The ``next-config.yml`` file should include a parameter named ``last_path``:

.. code-block:: yaml

    last_path: mydata/csv/sample_01.csv.gz

With this configuration, embulk loads the files newer than this file in alphabetical order.

For example, if you create ``./mydata/csv/sample_02.csv.gz`` file, embulk skips ``sample_01.csv.gz`` file and loads ``sample_02.csv.gz`` only next time. And the next next-config.yml file has ``last_path: mydata/csv/sample_02.csv.gz`` for the next next execution.

So, if you want to loads newly created files every day, you can setup this cron schedule:

.. code-block:: cron

    0 * * * * embulk run /path/to/next-config.yml -o /path/to/next-config.yml

