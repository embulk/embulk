Scheduled bulk data loading to Elasticsearch + Kibana 4 from CSV files
==================================

.. contents::
   :local:
   :depth: 2

This article shows how to:

* Bulk load CSV files to Elasticsearch.
* Visualize the data with Kibana interactively.
* Schedule the data loading every hour using cron.

This guide assumes you are using Ubuntu 12.04 Precise or Mac OS X.

Setup Elasticsearch and Kibana 4
------------------

Step 1. Download and start Elasticsearch.
~~~~~~~~~~~~~~~~~~

You can find releases from the `Elasticsearch website <http://www.elasticsearch.org/download/>`_.
For the smallest setup, you can unzip the package and run `./bin/elasticsearch` command:

.. code-block:: console

    $ wget https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/2.2.0/elasticsearch-2.2.0.zip
    $ unzip elasticsearch-2.2.0.zip
    $ cd elasticsearch-2.2.0
    $ ./bin/elasticsearch

Step 2. Download and unzip Kibana:
~~~~~~~~~~~~~~~~~~

You can find releases from the `Kibana website <http://www.elasticsearch.org/overview/kibana/installation/>`_. Open a new console and run following commands:

.. code-block:: console

    $ wget https://download.elastic.co/kibana/kibana/kibana-4.4.0-linux-x64.tar.gz
    $ tar zxvf kibana-4.4.0-linux-x64.tar.gz
    $ cd kibana-4.4.0-linux-x64
    $ ./bin/kibana

Note: If you're using Mac OS X, https://download.elastic.co/kibana/kibana/kibana-4.4.0-darwin-x64.tar.gz is the URL to download.

Now Elasticsearch and Kibana started. Open http://localhost:5601/ using your browser to see the Kibana's graphical interface.


Setup Embulk
------------------

Step 1. Download Embulk binary:
~~~~~~~~~~~~~~~~~~

You can find the latest embulk binary from the `releases <https://bintray.com/embulk/maven/embulk/view#files>`_. Because Embulk is a single executable binary, you can simply download it to /usr/local/bin directory and set executable flag as following:

.. code-block:: console

    $ sudo wget http://dl.embulk.org/embulk-latest.jar -O /usr/local/bin/embulk
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

Create this configuration file and save as ``seed.yml``:

.. code-block:: yaml

    in:
      type: file
      path_prefix: ./mydata/csv/
    out:
      type: elasticsearch
      index: embulk
      index_type: embulk
      nodes:
        - host: localhost

In fact, this configuration lacks some important information. However, embulk guesses the other information. So, next step is to order embulk to guess them:

.. code-block:: console

    $ embulk guess ./mydata/seed.yml -o config.yml

The generated config.yml file should include complete information as following:

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
        skip_header_lines: 1
        columns:
        - {name: id, type: long}
        - {name: account, type: long}
        - {name: time, type: timestamp, format: '%Y-%m-%d %H:%M:%S'}
        - {name: purchase, type: timestamp, format: '%Y%m%d'}
        - {name: comment, type: string}
    out:
      type: elasticsearch
      index: embulk
      index_type: embulk
      nodes:
      - {host: localhost}

Note: If the CSV file contains timestamp in local time zone, set default_timezone parameter to parser config as following (since time zone is assumed as UTC by default).

.. code-block:: yaml

      parser:
        default_timezone: 'Asia/Tokyo'

Now, you can run the bulk loading:

.. code-block:: console

    $ embulk run config.yml -c diff.yml

Scheduling loading by cron
------------------

At the last step, you ran embulk command with ``-c diff.yml`` file. The ``diff.yml`` file should include a parameter named ``last_path``:

.. code-block:: yaml

    in: {last_path: mydata/csv/sample_01.csv.gz}
    out: {}

With this configuration, embulk loads the files newer than this file in alphabetical order.

For example, if you create ``./mydata/csv/sample_02.csv.gz`` file, embulk skips ``sample_01.csv.gz`` file and loads ``sample_02.csv.gz`` only next time. And the next ``diff.yml`` file has ``last_path: mydata/csv/sample_02.csv.gz`` for the next next execution.

So, if you want to loads newly created files every day, you can setup this cron schedule:

.. code-block:: cron

    0 * * * * embulk run /path/to/config.yml -c /path/to/diff.yml

