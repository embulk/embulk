Configuration
==================================

.. contents::
   :local:
   :depth: 2

Embulk configuration file format
------------------

Embulk uses a YAML file to define a bulk data loading. Here is an example of the file:

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
        escape: '"'
        null_string: 'NULL'
        skip_header_lines: 1
        columns:
        - {name: id, type: long}
        - {name: account, type: long}
        - {name: time, type: timestamp, format: '%Y-%m-%d %H:%M:%S'}
        - {name: purchase, type: timestamp, format: '%Y%m%d'}
        - {name: comment, type: string}
    filters:
      - type: speedometer
        speed_limit: 250000
    out:
      type: stdout

A configuration file consists of following sections:

* **in:** Input plugin options. An input plugin is either record-based (`MySQL <https://github.com/embulk/embulk-input-jdbc>`_, `DynamoDB <https://github.com/lulichn/embulk-input-dynamodb>`_, etc) or file-based (`S3 <https://github.com/embulk/embulk-input-s3>`_, `HTTP <https://github.com/takumakanari/embulk-input-http>`_, etc).

  * **parser:** If the input is file-based, parser plugin parses a file format (built-in csv, `json <https://github.com/takumakanari/embulk-parser-json>`_, etc).

  * **decoder:** If the input is file-based, decoder plugin decodes compression or encryption (built-in gzip, `zip <https://github.com/hata/embulk-decoder-commons-compress>`_, `tar.gz <https://github.com/hata/embulk-decoder-commons-compress>`_, etc).

* **out:** Output plugin options. An output plugin is either record-based (`Oracle <https://github.com/embulk/embulk-output-jdbc>`_, `Elasticsearch <https://github.com/muga/embulk-output-elasticsearch>`_, etc) or file-based (`Google Cloud Storage <https://github.com/hakobera/embulk-output-gcs>`_, `Command <https://github.com/embulk/embulk-output-command>`_, etc)

  * **formatter:** If the output is file-based, formatter plugin formats a file format (such as built-in csv, `JSON <https://github.com/takei-yuya/embulk-formatter-jsonl>`_)

  * **encoder:** If the output is file-based, encoder plugin encodes compression or encryption (such as built-in gzip)

* **filters:** Filter plugins options (optional).

* **exec:** Executor plugin options. An executor plugin control parallel processing (such as built-in thread executor, `Hadoop MapReduce executor <https://github.com/embulk/embulk-executor-mapreduce>`_)

In many cases, what you need to write is **in:**, **out**: and **formatter** sections only because ``guess`` command guesses **parser** and **decoder** options for you. See also the `Quick Start <https://github.com/embulk/embulk#quick-start>`_.


Using variables
~~~~~~~~~~~~~~~~~~

You can embed environment variables in configuration file using `Liquid template engine <http://liquidmarkup.org/>`_ (This is experimental feature. Behavior might change or be removed in future releases).

To use template engine, configuration file name must end with ``.yml.liquid``.

Environment variables are set to ``env`` variable.

.. code-block:: yaml

    in:
      type: file
      path_prefix: {{ env.path_prefix }}
      decoders:
      - {type: gzip}
      parser:
        ...
    out:
      type: postgresql
      host: {{ env.pg_host }}
      port: {{ env.pg_port }}
      user: {{ env.pg_user }}
      password: "{{ env.pg_password }}"
      database: embulk_load
      mode: insert
      table: {{ env.pg_table }}


Local file input plugin
------------------

The ``file`` input plugin reads files from local file system.

Options
~~~~~~~~~~~~~~~~~~

+----------------+----------+------------------------------------------------+-----------+
| name           | type     | description                                    | required? |
+================+==========+================================================+===========+
| path\_prefix   | string   | Path prefix of input files                     | required  |
+----------------+----------+------------------------------------------------+-----------+
| parsers        | hash     | Parsers configurations (see below)             | required  |
+----------------+----------+------------------------------------------------+-----------+
| decoders       | array    | Decoder configuration (see below)              |           |
+----------------+----------+------------------------------------------------+-----------+
| last\_path     | string   | Name of last read file in previous operation   |           |
+----------------+----------+------------------------------------------------+-----------+

The ``path_prefix`` option is required. If you have files as following, you may set ``path_prefix: /path/to/files/sample_``:

::

    .
    `-- path
        `-- to
            `-- files
                |-- sample_01.csv   -> read
                |-- sample_02.csv   -> read
                |-- sample_03.csv   -> read
                |-- sample_04.csv   -> read

The ``last_path`` option is used to skip files older than or same with the file in dictionary order.
For example, if you set ``last_path: /path/to/files/sample_02.csv``, Embulk reads following files:

::

    .
    `-- path
        `-- to
            `-- files
                |-- sample_01.csv   -> skip
                |-- sample_02.csv   -> skip
                |-- sample_03.csv   -> read
                |-- sample_04.csv   -> read

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    in:
      type: file
      path_prefix: /path/to/files/sample_
      last_path: /path/to/files/sample_02.csv
      parser:
        ...

In most of cases, you'll use guess to configure the parsers and decoders. See also `Quick Start <https://github.com/embulk/embulk#quick-start>`_.

CSV parser plugin
------------------

The ``csv`` parser plugin parses CSV and TSV files.

Options
~~~~~~~~~~~~~~~~~~

+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| name                       | type     | description                                                                                                    |              required? |
+============================+==========+================================================================================================================+========================+
| delimiter                  | string   | Delimiter character such as ``,`` for CSV, ``"\t"`` for TSV, ``"|"`` or any single-byte character              | ``,`` by default       |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| quote                      | string   | The character surrounding a quoted value. Setting ``null`` disables quoting.                                   | ``\"`` by default      |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| escape                     | string   | Escape character to escape a special character. Setting ``null`` disables escaping.                            | ``\\`` by default      |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| skip\_header\_lines        | integer  | Skip this number of lines first. Set 1 if the file has header line.                                            | ``0`` by default       |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| null\_string               | string   | If a value is this string, converts it to NULL. For example, set ``\N`` for CSV files created by mysqldump     |                        |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| trim\_if\_not\_quoted      | boolean  | If true, remove spaces of a value if the value is not surrounded by the quote character                        | ``false`` by default   |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| comment\_line\_marker      | string   | Skip a line if the line begins with this string                                                                | null by default        |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| allow\_optional\_columns   | boolean  | If true, set null to insufficient columns. Otherwise, skip the row in case of insufficient number of columns   | ``false`` by default   |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| allow\_extra\_columns      | boolean  | If true, ignore too many columns. Otherwise, skip the row in case of too many columns                          | ``false`` by default   |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| max\_quoted\_size\_limit   | integer  | Maximum number of bytes of a quoted value. If a value exceeds the limit, the row will be skipped               | ``131072`` by default  |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| default\_timezone          | string   | Time zone of timestamp columns if the value itself doesn't include time zone description (eg. Asia/Tokyo)      | ``UTC`` by default     |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| newline                    | enum     | Newline character (CRLF, LF or CR)                                                                             | ``CRLF`` by default    |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| charset                    | enum     | Character encoding (eg. ISO-8859-1, UTF-8)                                                                     | ``UTF-8`` by default   |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+
| columns                    | hash     | Columns (see below)                                                                                            | required               |
+----------------------------+----------+----------------------------------------------------------------------------------------------------------------+------------------------+

The ``columns`` option declares the list of columns. This CSV parser plugin ignores the header line.

+----------+-------------------------------------------------+
| name     | description                                     |
+==========+=================================================+
| name     | Name of the column                              |
+----------+-------------------------------------------------+
| type     | Type of the column (see below)                  |
+----------+-------------------------------------------------+
| format   | Format of the timestamp if type is timestamp    |
+----------+-------------------------------------------------+

List of types:

+-------------+----------------------------------------------+
| name        | description                                  |
+=============+==============================================+
| boolean     | true or false                                |
+-------------+----------------------------------------------+
| long        | 64-bit signed integers                       |
+-------------+----------------------------------------------+
| timestamp   | Date and time with nano-seconds precision    |
+-------------+----------------------------------------------+
| double      | 64-bit floating point numbers                |
+-------------+----------------------------------------------+
| string      | Strings                                      |
+-------------+----------------------------------------------+

You can use ``guess`` to automatically generate the column settings. See also `Quick Start <https://github.com/embulk/embulk#quick-start>`_.

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    in:
      ...
      parser:
        type: csv
        charset: UTF-8
        newline: CRLF
        delimiter: "\t"
        quote: '"'
        escape: '"'
        null_string: 'NULL'
        skip_header_lines: 1
        comment_line_marker: '#'
        columns:
        - {name: id, type: long}
        - {name: account, type: long}
        - {name: time, type: timestamp, format: '%Y-%m-%d %H:%M:%S'}
        - {name: purchase, type: timestamp, format: '%Y%m%d'}
        - {name: comment, type: string}

Gzip decoder plugin
------------------

The ``gzip`` decoder plugin decompresses gzip files before input plugins read them.

Options
~~~~~~~~~~~~~~~~~~

This plugin doesn't have any options.

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    in:
      ...
      decoders:
      - {type: gzip}


File output plugin
------------------

The ``file`` output plugin writes records to local file system.

Options
~~~~~~~~~~~~~~~~~~

+--------------------+----------+---------------------------------------------------+----------------------------+
| name               | type     | description                                       | required?                  |
+====================+==========+===================================================+============================+
| path\_prefix       | string   | Path prefix of the output files                   | required                   |
+--------------------+----------+---------------------------------------------------+----------------------------+
| sequence\_format   | string   | Format of the sequence number of the output files | ``%03d.%02d.`` by default  |
+--------------------+----------+---------------------------------------------------+----------------------------+
| file\_ext          | string   | Path suffix of the output files (e.g. ``"csv"``)  | required                   |
+--------------------+----------+---------------------------------------------------+----------------------------+

For example, if you set ``path_prefix: /path/to/output/sample_``, ``sequence_format: "%03d.%02d."``, and ``file_ext: csv``, name of the output files will be as following:

::

    .
    `-- path
        `-- to
            `-- output
                |-- sample_01.000.csv
                |-- sample_02.000.csv
                |-- sample_03.000.csv
                |-- sample_04.000.csv

``sequence_format`` formats task index and sequence number in a task.

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    out:
      type: file
      path_prefix: /path/to/output/sample_
      file_ext: csv
      formatter:
        ...

CSV formatter plugin
------------------

The ``csv`` formatter plugin formats records using CSV or TSV format.

Options
~~~~~~~~~~~~~~~~~~

+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| name                 | type    | description                                                                                           | required?              |
+======================+=========+=======================================================================================================+========================+
| delimiter            | string  | Delimiter character such as ``,`` for CSV, ``"\t"`` for TSV, ``"|"`` or any single-byte character     | ``,`` by default       |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| quote                | string  | The character surrounding a quoted value                                                              | ``\"`` by default      |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| quote\_policy        | enum    | Policy for quote (ALL, MINIMAL, NONE) (see below)                                                     | ``MINIMAL`` by default |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| escape               | string  | Escape character to escape a quote character when quote\_policy is ALL or MINIMAL                     | ``\"`` by default      |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| header\_line         | boolean | If true, write the header line with column name at the first line                                     | ``true`` by default    |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| null_string          | string  | Expression of NULL values                                                                             | empty by default       |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| newline              | enum    | Newline character (CRLF, LF or CR)                                                                    | ``CRLF`` by default    |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| newline\_in\_field   | enum    | Newline character in each field (CRLF, LF, CR)                                                        | ``LF`` by default      |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| charset              | enum    | Character encoding (eg. ISO-8859-1, UTF-8)                                                            | ``UTF-8`` by default   |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| default\_timezone    | string  | Time zone of timestamp columns. This can be overwritten for each column using ``column_options``      | ``UTC`` by default     |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+
| column\_options      | hash    | See bellow                                                                                            | optional               |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+------------------------+

The ``quote_policy`` option is used to determine field type to quote.

+------------+--------------------------------------------------------------------------------------------------------+
| name       | description                                                                                            |
+============+========================================================================================================+
| ALL        | Quote all fields                                                                                       |
+------------+--------------------------------------------------------------------------------------------------------+
| MINIMAL    | Only quote those fields which contain delimiter, quote or any of the characters in lineterminator      |
+------------+--------------------------------------------------------------------------------------------------------+
| NONE       | Never quote fields. When the delimiter occurs in field, escape with escape char                        |
+------------+--------------------------------------------------------------------------------------------------------+

The ``column_options`` option is a map whose keys are name of columns, and values are configuration with following parameters:

+----------------------+---------+-------------------------------------------------------------------------------------------------------+-----------------------------------------+
| name                 | type    | description                                                                                           | required?                               |
+======================+=========+=======================================================================================================+=========================================+
| timezone             | string  | Time zone if type of this column is timestamp. If not set, ``default\_timezone`` is used.             | optional                                |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-----------------------------------------+
| format               | string  | Timestamp format if type of this column is timestamp.                                                 | ``%Y-%m-%d %H:%M:%S.%6N %z`` by default |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-----------------------------------------+

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    out:
      ...
      formatter:
        type: csv
        delimiter: '\t'
        newline: CRLF
        newline_in_field: LF
        charset: UTF-8
        quote_policy: MINIMAL
        quote: '"'
        escape: '\\'
        null_string: '\\N'
        default_timezone: 'UTC'
        column_options:
          mycol1: {format: '%Y-%m-%d %H:%M:%S'}
          mycol2: {format: '%Y-%m-%d %H:%M:%S', timezone: 'America/Los_Angeles'}

Gzip encoder plugin
------------------

The ``gzip`` encoder plugin compresses output files using gzip.

Options
~~~~~~~~~~~~~~~~~~

+---------+----------+----------------------------------------------------------------------+--------------------+
| name    | type     | description                                                          | required?          |
+=========+==========+======================================================================+====================+
| level   | integer  | Compression level. From 0 (no compression) to 9 (best compression).  | ``6`` by default   |
+---------+----------+----------------------------------------------------------------------+--------------------+

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    out:
      ...
      encoders:
      - type: gzip
        level: 1

Rename filter plugin
------------------

The ``rename`` filter plugin changes column names. This plugin has no impact on performance.

Options
~~~~~~~~~~~~~~~~~~

+---------+----------+----------------------------------------------------------------------+--------------------+
| name    | type     | description                                                          | required?          |
+=========+==========+======================================================================+====================+
| columns | hash     | A map whose keys are existing column names. values are new names.    | ``{}`` by default  |
+---------+----------+----------------------------------------------------------------------+--------------------+

Example
~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    filters:
      ...
      - type: rename
	columns:
	  my_existing_column1: new_column1
	  my_existing_column2: new_column2

