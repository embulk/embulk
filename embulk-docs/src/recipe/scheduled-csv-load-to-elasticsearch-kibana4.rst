Scheduled bulk data loading to Elasticsearch + Kibana 4 from CSV files
==================================

This article shows how to:

* Collect CSV files to Elasticsearch.
* Schedule the data loading every hour using cron.
* Visualize the data with Kibana in real-time.

This guide assumes you are using Ubuntu 12.0 Precise.

Setup: Elasticsearch and Kibana 4
------------------

Step 1. Download and start Elasticsearch.
~~~~~~~~~~~~~~~~~~

You can find releases from [Elasticsearch website](http://www.elasticsearch.org/download/).
For the smallest setup, you can unzip the package and run `./bin/elasticsearch` command:

    $ wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.4.zip
    $ unzip elasticsearch-1.4.4.zip
    $ cd elasticsearch-1.4.4
    $ ./bin/elasticsearch

Step 2. Download and unzip Kibana:
~~~~~~~~~~~~~~~~~~

You can find releases from [Kibana website](http://www.elasticsearch.org/overview/kibana/installation/). Open new terminal and run following commands:

    $ wget https://download.elasticsearch.org/kibana/kibana/kibana-4.0.0-linux-x64.tar.gz
    $ tar zxvf kibana-4.0.0-linux-x64.tar.gz
    $ ./bin/kibana

Now Elasticsearch and Kibana started. Open http://localhost:5601/ using your browser to see the Kibana's graphical interface.


Setup: Embulk
------------------

Step 1. Download Embulk binary:
~~~~~~~~~~~~~~~~~~

You can find the latest embulk binary from the [releases](https://bintray.com/embulk/maven/embulk/view#files). Because Embulk is a single executable binary, you can simply download it to /usr/local/bin directory and set executable flag as following:

    $ sudo wget https://bintray.com/artifact/download/embulk/maven/embulk-0.4.5.jar -O /usr/local/bin/embulk
    $ sudo chmod +x /usr/local/bin/embulk

Step 2. Install Elasticsearch plugin
~~~~~~~~~~~~~~~~~~

To load data to Elasticsearch, you also need Elasticsearch plugin for Embulk. You can install the plugin with this command:

    $ embulk gem install embulk-output-elasticsearch

Embulk includes CSV file reader in itself. Now everything is ready to use.

Loading a CSV file
------------------

Assuming you have a CSV files at ``./mydata/csv/`` directory. If you don't have CSV files, you can create ones using ``embulk example ./mydata`` command.

Create this configuration file named ``config.yml``:

    in:
      type: file
      path_prefix: ./mydata/csv/
    out:
      type: elasticsearch
      index_name: embulk
      index_type: embulk
      nodes:
        - host: localhost

In fact, this configuration file lacks some important information. However, embulk guesses the other information. So, next step is to order embulk to guess them:

    $ embulk guess config.yml -o config-complete.yml

The created config-complete.yml file should include complete information to run the loading as following:


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

    $ embulk run config-complete.yml -o next-config.yml

Scheduling bulk loading by cron
------------------

At the last step, you ran embulk command with ``-o next-config.yml`` file. This file should include a parameter named ``last_path``:

    last_path: mydata/csv/sample_01.csv.gz

With this configuration, embulk loads the files newer than this file in alphabetical order.
For example, if you create ``./mydata/csv/sample_02.csv.gz`` file, embulk skips ``sample_01.csv.gz`` file and loads ``sample_02.csv.gz`` only. And the next next-config.yml file has ``last_path: mydata/csv/sample_01.csv.gz`` for the next next execution.

So, if you want to loads newly created files, you can setup this cron schedule:

    0 * * * * embulk run /path/to/next-config.yml -o /path/to/next-config.yml

