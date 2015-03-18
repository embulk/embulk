
Built-in plugin
###############


Embulk are expressed in YAML format. Here is an example configuration.

::

    in:
      type: file
      path_prefix: /data/embulk/datas/access_log-
      decoders:
        - type: gzip
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
    # filters:
    out:
      type: stdout

Embulk are consists of many plugins, You can use various plugin at
different stages.

Currently, It support the following plugin types.

-  input: input plugin (you also use decoder and parser).
-  output: output plugins ( you also use encoder and formatter )
-  filters: filter plugins (no filter plugin available in built-in
   plugins)
-  exec: executer (not implemented yet)

Input plugins
*************

Embulk read datas as record from various datasource (ex. RDBMS, cloud
service or local storage)

The ``type`` is a name of plugin type.

::

    in:
      type: # write input plugin type here

Currently, input plugin is ``file`` plugin only. It's read data from
local storage.

File plugin.
============

``file`` plugin are support the following configurations.

+----------------+----------+------------------------------------------------+
| name           | type     | description                                    |
+================+==========+================================================+
| path\_prefix   | string   | path prefix of input files                     |
+----------------+----------+------------------------------------------------+
| decoders       | array    | decoder configuration (see below)              |
+----------------+----------+------------------------------------------------+
| parsers        | hash     | parsers configurations (see below)             |
+----------------+----------+------------------------------------------------+
| last\_path     | string   | name of last read file in previous operation   |
+----------------+----------+------------------------------------------------+

Generally, you can use ``guess`` command to generate various
configurations automatically.

At least, you must specify ``path_prefix`` which is place of data only.

If you have datas like the below, your path\_prefix are
``path_prefix: /path/to/datas/sample_``

::

    .
    `-- path
        `-- to
            `-- datas
                |-- sample_01.csv
                |-- sample_02.csv
                |-- sample_03.csv
                |-- sample_04.csv
                |-- sample_05.csv
                |-- sample_06.csv
                |-- sample_07.csv
                |-- sample_08.csv
                |-- sample_09.csv
                `-- sample_10.csv

The ``last_path`` is the record of final reading file at previous
execution. If this path is set, it read the next file of last\_path
files. (Note Embulk read input file Unicode order, so input file name
must name ascending order)

Decoders
========

Typically, various data are compressed by compress program (ex. gzip).
and you want to decode those datas. Another case, some data are
encrypted, and you might want to decrypt it.

A decoder plugin decode those data files.

Gzip decoder are available in built-in plugins.

+--------+---------------------------+
| name   | description               |
+========+===========================+
| gzip   | decompress gzip'ed data   |
+--------+---------------------------+

This decoder is no configuration option.

Parsers
========

Parser plugin parse data as record. You can use CSV parser in build-in
plugin.

+--------+------------------------+
| name   | description            |
+========+========================+
| csv    | read csv(tsv) format   |
+--------+------------------------+

CSV parser plugins
------------------

+----------------------------+-----------------------------------------------+
| name                       | description                                   |
+============================+===============================================+
| delimiter                  | delimiter character ex) CSV ",", TSV "\\t"    |
+----------------------------+-----------------------------------------------+
| quote                      | quote character, use data as string           |
+----------------------------+-----------------------------------------------+
| escape                     | escape character to use quote character       |
+----------------------------+-----------------------------------------------+
| null\_string               | null string                                   |
+----------------------------+-----------------------------------------------+
| header\_line(Obsolate)     | true,false use as header line first row       |
+----------------------------+-----------------------------------------------+
| skip\_header\_lines        | number of skip header lines(v0.5.2)           |
+----------------------------+-----------------------------------------------+
| newline                    | newline characters CR, CRLF and LF            |
+----------------------------+-----------------------------------------------+
| columns                    | columns configuration (see below)             |
+----------------------------+-----------------------------------------------+
| max\_quoted\_size\_limit   | look-ahead size when after quoted character   |
+----------------------------+-----------------------------------------------+
| default\_timezone          | default time zone config ex. Asia/Tokyo       |
+----------------------------+-----------------------------------------------+
| trim\_if\_not\_quoted      | true,false trim spaces if not quoted          |
+----------------------------+-----------------------------------------------+
| charset                    | input character sets, ex. ISO-8859-1, UTF-8   |
+----------------------------+-----------------------------------------------+

columns configuration.

+----------+------------------------------------------+
| name     | description                              |
+==========+==========================================+
| name     | name of column                           |
+----------+------------------------------------------+
| type     | type of column                           |
+----------+------------------------------------------+
| format   | timestamp format. type: timestamp only   |
+----------+------------------------------------------+

types.

+-------------+------------------+
| name        | description      |
+=============+==================+
| boolean     | true / false     |
+-------------+------------------+
| long        | integer number   |
+-------------+------------------+
| timestamp   | date and time    |
+-------------+------------------+
| double      | floating value   |
+-------------+------------------+
| string      | string           |
+-------------+------------------+

Output plugin
*************

You can use three types output plugins.

+----------+-------------------+
| name     | description       |
+==========+===================+
| stdout   | standard output   |
+----------+-------------------+
| file     | file output       |
+----------+-------------------+
| null     | dummy output      |
+----------+-------------------+

stdout and null output are no options.

::

    #out: {type: stdout}
    #out: {type: null}
    out:
      type: file
      path_prefix: ./sample
      file_ext: .csv.gz
      formatter:
        type: csv
        header_line: true
        charset: UTF-8
        newline: CRLF
      encoders:
      - {type: gzip, level: 6 }

file output
===========

+--------------------+----------+-------------------------------------------------+-------------+
| name               | type     | description                                     | note        |
+====================+==========+=================================================+=============+
| path\_prefix       | string   | path prefix of input files                      |             |
+--------------------+----------+-------------------------------------------------+-------------+
| sequence\_format   | string   | task and file index sequential values formats   | .03d.%02d   |
+--------------------+----------+-------------------------------------------------+-------------+
| file\_ext          | string   | parsers configurations (see below)              |             |
+--------------------+----------+-------------------------------------------------+-------------+
| encoders           | array    | name of last read file in previous operation    |             |
+--------------------+----------+-------------------------------------------------+-------------+
| formatter          | hash     | formatter configuration (see below)             |             |
+--------------------+----------+-------------------------------------------------+-------------+

For example, when you use this configuration,

::

    out:
      type: file
      path_prefix: /path/to/datas/sample
      file_ext: csv

It output

::

    .
    `-- path
        `-- to
            `-- datas
                |-- sample.01.000.csv
                |-- sample.02.000.csv
                |-- sample.03.000.csv
                |-- sample.04.000.csv
                |-- sample.05.000.csv
                |-- sample.06.000.csv
                |-- sample.07.000.csv
                |-- sample.08.000.csv
                |-- sample.09.000.csv
                `-- sample.10.000.csv

``sequence_format`` are pair of task and file index. If you use file input
plugin, number of files use as task index. Currently file index are
always 0,

Formatter
=========

You can use CSV formatter.

+----------------+-------------------------------------------+
| name           | description                               |
+================+===========================================+
| newline        | newline string. CR, CRLF, LF              |
+----------------+-------------------------------------------+
| charset        | charset, ex. ISO-8859-1, UTF-8            |
+----------------+-------------------------------------------+
| header\_line   | true,false write header line              |
+----------------+-------------------------------------------+
| delimiter      | delimiter character ex) CSV ",", TSV "\\t"|
+----------------+-------------------------------------------+

Encoders
========

You can use gzip encoder

+---------+--------------------------------------+
| name    | description                          |
+=========+======================================+
| level   | 9 best, 0 (no compress), default 6   |
+---------+--------------------------------------+
