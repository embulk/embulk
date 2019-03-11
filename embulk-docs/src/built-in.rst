Configuration
==============

.. contents::
   :local:
   :depth: 2

Embulk configuration file format
---------------------------------

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

  * **decoder:** If the input is file-based, decoder plugin decodes compression or encryption (built-in gzip, bzip2, `zip <https://github.com/hata/embulk-decoder-commons-compress>`_, `tar.gz <https://github.com/hata/embulk-decoder-commons-compress>`_, etc).

* **out:** Output plugin options. An output plugin is either record-based (`Oracle <https://github.com/embulk/embulk-output-jdbc>`_, `Elasticsearch <https://github.com/muga/embulk-output-elasticsearch>`_, etc) or file-based (`Google Cloud Storage <https://github.com/hakobera/embulk-output-gcs>`_, `Command <https://github.com/embulk/embulk-output-command>`_, etc)

  * **formatter:** If the output is file-based, formatter plugin formats a file format (such as built-in csv, `jsonl <https://github.com/takei-yuya/embulk-formatter-jsonl>`_)

  * **encoder:** If the output is file-based, encoder plugin encodes compression or encryption (such as built-in gzip or bzip2)

* **filters:** Filter plugins options (optional).

* **exec:** Executor plugin options. An executor plugin control parallel processing (such as built-in thread executor, `Hadoop MapReduce executor <https://github.com/embulk/embulk-executor-mapreduce>`_)

In many cases, what you need to write is **in:**, **out**: and **formatter** sections only because ``guess`` command guesses **parser** and **decoder** options for you. See also the `Quick Start <https://github.com/embulk/embulk#quick-start>`_.


Using variables
~~~~~~~~~~~~~~~~

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


Including files
~~~~~~~~~~~~~~~~

Configuration file can include another configuration file. To use it, configuration file name must end with ``.yml.liquid``.

File will be searched from the relative path of the input configuration file. And file name will be ``_<name>.yml.liquid``. For example, if you add ``{% include 'subdir/inc' %}`` tag to ``myconfig/config.yml.liquid`` file, it includes ``myconfig/subdir/_inc.yml.liquid`` file.

.. code-block:: liquid

    # config.yml.liquid
    {% include 'in_mysql' %}
    out:
      type: stdout

.. code-block:: yaml

    # _in_mysql.yml.liquid
    in:
      type: mysql

With above 2 files, actual configuration file will be:

.. code-block:: yaml

    # $ embulk run config.yml.liquid
    in:
      type: mysql
    out:
      type: stdout



Local file input plugin
------------------------

The ``file`` input plugin reads files from local file system.

Options
~~~~~~~~

+------------------+----------+------------------------------------------------+-----------------------+
| name             | type     | description                                    | required?             |
+==================+==========+================================================+=======================+
| path\_prefix     | string   | Path prefix of input files                     | required              |
+------------------+----------+------------------------------------------------+-----------------------+
| parser           | hash     | Parser configuration (see below)               | required              |
+------------------+----------+------------------------------------------------+-----------------------+
| decoders         | array    | Decoder configuration (see below)              |                       |
+------------------+----------+------------------------------------------------+-----------------------+
| last\_path       | string   | Name of last read file in previous operation   |                       |
+------------------+----------+------------------------------------------------+-----------------------+
| follow\_symlinks | boolean  | If `true`, follow symbolic link directories    | ``false`` by default  |
+------------------+----------+------------------------------------------------+-----------------------+

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
~~~~~~~~

.. code-block:: yaml

    in:
      type: file
      path_prefix: /path/to/files/sample_
      last_path: /path/to/files/sample_02.csv
      parser:
        ...

In most of cases, you'll use guess to configure the parser and decoders. See also `Quick Start <https://github.com/embulk/embulk#quick-start>`_.

CSV parser plugin
------------------

The ``csv`` parser plugin parses CSV and TSV files.

Options
~~~~~~~~

+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| name                        | type     | description                                                                                                             |                               required?    |
+=============================+==========+=========================================================================================================================+============================================+
| delimiter                   | string   | Delimiter character such as ``,`` for CSV, ``"\t"`` for TSV, ``"|"``                                                    | ``,`` by default                           |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| quote                       | string   | The character surrounding a quoted value. Setting ``null`` disables quoting.                                            | ``"`` by default                           |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| escape                      | string   | Escape character to escape a special character. Setting ``null`` disables escaping.                                     | ``\\`` by default                          |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| skip\_header\_lines         | integer  | Skip this number of lines first. Set 1 if the file has header line.                                                     | ``0`` by default                           |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| null\_string                | string   | If a value is this string, converts it to NULL. For example, set ``\N`` for CSV files created by mysqldump              |                                            |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| trim\_if\_not\_quoted       | boolean  | If true, remove spaces of a value if the value is not surrounded by the quote character                                 | ``false`` by default                       |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| quotes\_in\_quoted\_fields  | enum     | Specify how to deal with irregular unescaped quote characters in quoted fields                                          | ``ACCEPT_ONLY_RFC4180_ESCAPED`` by default |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| comment\_line\_marker       | string   | Skip a line if the line begins with this string                                                                         | null by default                            |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| allow\_optional\_columns    | boolean  | If true, set null to insufficient columns. Otherwise, skip the row in case of insufficient number of columns            | ``false`` by default                       |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| allow\_extra\_columns       | boolean  | If true, ignore too many columns. Otherwise, skip the row in case of too many columns                                   | ``false`` by default                       |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| max\_quoted\_size\_limit    | integer  | Maximum number of bytes of a quoted value. If a value exceeds the limit, the row will be skipped                        | ``131072`` by default                      |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| stop\_on\_invalid\_record   | boolean  | Stop bulk load transaction if a file includes invalid record (such as invalid timestamp)                                | ``false`` by default                       |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| default\_timezone           | string   | Time zone of timestamp columns if the value itself doesn't include time zone description (eg. Asia/Tokyo)               | ``UTC`` by default                         |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| default\_date               | string   | Set date part if the format doesn’t include date part.                                                                  | ``1970-01-01`` by default                  |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| newline                     | enum     | CRLF, LF or CR. This value is inserted into multi-line quoted value as newline character                                | ``CRLF`` by default                        |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| line\_delimiter\_recognized | enum     | CRLF, LF or CR. If specified, only the character is recognized as line delimiter. Otherwise, all of them are recognized |                                            |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| charset                     | enum     | Character encoding (eg. ISO-8859-1, UTF-8)                                                                              | ``UTF-8`` by default                       |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+
| columns                     | hash     | Columns (see below)                                                                                                     | required                                   |
+-----------------------------+----------+-------------------------------------------------------------------------------------------------------------------------+--------------------------------------------+

The ``quotes_in_quoted_fields`` option specifies how to deal with irregular non-escaped stray quote characters.

+------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+
| name                                                 | description                                                                                                                                         |
+======================================================+=====================================================================================================================================================+
| ACCEPT_ONLY_RFC4180_ESCAPED                          | Default. Accept only specified and RFC 4180-style escaped quote characters.                                                                         |
+------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+
| ACCEPT_STRAY_QUOTES_ASSUMING_NO_DELIMITERS_IN_FIELDS | Accept stray quotes as-is in the field. Instead, it behaves undefined if delimiters are in fields. ``"a"b"`` goes ``a"b``. ``"a""b"`` goes ``a"b``. |
+------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+

The ``columns`` option declares the list of columns. This CSV parser plugin ignores the header line.

+----------+--------------------------------------------------------+
| name     | description                                            |
+==========+========================================================+
| name     | Name of the column                                     |
+----------+--------------------------------------------------------+
| type     | Type of the column (see below)                         |
+----------+--------------------------------------------------------+
| format   | Format of the timestamp if type is timestamp           |
+----------+--------------------------------------------------------+
| date     | Set date part if the format doesn’t include date part  |
+----------+--------------------------------------------------------+

.. note::

   The Timestamp format refers to `Ruby strftime format <https://docs.ruby-lang.org/en/2.4.0/Date.html#method-i-strftime>`_

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

The ``null_string`` option converts certain values to NULL. Values will be converted as following:

+---------------------------------+-------------------------+--------------------------+----------------+--------------------+
|                                 | non-quoted empty string | quoted empty string ("") | non-quoted \\N | quoted \\N ("\\N") |
+=================================+=========================+==========================+================+====================+
| ``null_string: ""``             | NULL                    |  NULL                    | ``\N``         | ``\N``             |
+---------------------------------+-------------------------+--------------------------+----------------+--------------------+
| ``null_string: \N``             | (empty string)          |  (empty string)          | NULL           | NULL               |
+---------------------------------+-------------------------+--------------------------+----------------+--------------------+
| ``null_string: null`` (default) | NULL                    |  (empty string)          | ``\N``         | ``\N``             |
+---------------------------------+-------------------------+--------------------------+----------------+--------------------+

You can use ``guess`` to automatically generate the column settings. See also `Quick Start <https://github.com/embulk/embulk#quick-start>`_.

Example
~~~~~~~~

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


.. note::

    CSV parser supports ``format: '%s'`` to parse UNIX timestamp in seconds (e.g. 1470148959) as timestamp.

    However, CSV parser itself can't parse UNIX timestamp in millisecond (e.g. 1470148959542) as timestamp. You can still parse the column as ``long`` type first, then apply `timestamp_format <https://github.com/sonots/embulk-filter-timestamp_format>`_ filter plugin to convert long to timestamp. Here is an example:

    .. code-block:: yaml

       in:
         type: file
         path_prefix: /my_csv_files
         parser:
           ...
           columns:
           - {name: timestamp_in_seconds, type: timestamp, format: '%s'}
           - {name: timestamp_in_millis, type: long}
       filters:
         - type: timestamp_format
           columns:
             - {name: timestamp_in_millis, from_unit: ms}


JSON parser plugin
-------------------

The ``json`` parser plugin parses a JSON file that contains a sequence of JSON objects. Example:

.. code-block:: javascript

    {"time":1455829282,"ip":"93.184.216.34","name":"frsyuki"}
    {"time":1455829282,"ip":"172.36.8.109", "name":"sadayuki"}
    {"time":1455829284,"ip":"example.com","name":"Treasure Data"}
    {"time":1455829282,"ip":"10.98.43.1","name":"MessagePack"}

``json`` parser plugin outputs a single record named "record" (type is json).

Options
~~~~~~~~

+----------------------------+----------+-----------------------------------------------------------------------------------------------------------------+----------------------------+
| name                       | type     | description                                                                                                     |          required?         |
+============================+==========+=================================================================================================================+============================+
| stop\_on\_invalid\_record  | boolean  | Stop bulk load transaction if a file includes invalid record (such as invalid json)                             | ``false`` by default       |
+----------------------------+----------+-----------------------------------------------------------------------------------------------------------------+----------------------------+
| invalid\_string\_escapes   | enum     | Escape strategy of invalid json string such as using invalid ``\`` like ``\a``. (PASSTHROUGH, SKIP, UNESCAPE)   | ``PASSTHROUGH`` by default |
+----------------------------+----------+-----------------------------------------------------------------------------------------------------------------+----------------------------+
| root                       | string   | Specify when pointing a JSON object value as a record via JSON pointer expression                               | optional                   |
+----------------------------+----------+-----------------------------------------------------------------------------------------------------------------+----------------------------+
| flatten_json_array         | boolean  | Set true when regard elements in a JSON array as multiple Embulk records.                                       | ``false`` by default       |
+----------------------------+----------+-----------------------------------------------------------------------------------------------------------------+----------------------------+
| columns                    | hash     | Columns (see below)                                                                                             | optional                   |
+----------------------------+----------+-----------------------------------------------------------------------------------------------------------------+----------------------------+


if you set invalid\_string\_escapes and appear invalid JSON string (such as ``\a``), it makes following the action.

+----------------------------+------------------+
| invalid\_string\_escapes   | convert to       |
+============================+==================+
| PASSTHROUGH *1             | ``\a``           |
+----------------------------+------------------+
| SKIP                       | empty string     |
+----------------------------+------------------+
| UNESCAPE                   | ``a``            |
+----------------------------+------------------+

(\*1): Throwing an exception.

The ``columns`` option declares the list of columns, and the way how to extract JSON values into Embulk columns.

+-------------+----------------------------------------------------------------------------------------------------+
| name        | description                                                                                        |
+=============+====================================================================================================+
| name        | Name of the column. The JSON value with this name is extracted if `element_at` is not specified.   |
+-------------+----------------------------------------------------------------------------------------------------+
| type        | Type of the column (same as CSV parser's one)                                                      |
+-------------+----------------------------------------------------------------------------------------------------+
| element\_at | Descendant element to be extracted as the column, expressed as a relative JSON Pointer (optional). |
+-------------+----------------------------------------------------------------------------------------------------+
| format      | Format of the timestamp if type is timestamp                                                       |
+-------------+----------------------------------------------------------------------------------------------------+

Example
~~~~~~~~

.. code-block:: yaml

    in:
      parser:
        type: json
        columns:
        - {name: time, type: timestamp, format: "%s"}
        - {name: ip, type: string}
        - {name: name, type: string}

Gzip decoder plugin
--------------------

The ``gzip`` decoder plugin decompresses gzip files before input plugins read them.

Options
~~~~~~~~

This plugin doesn't have any options.

Example
~~~~~~~~

.. code-block:: yaml

    in:
      ...
      decoders:
      - {type: gzip}


BZip2 decoder plugin
---------------------

The ``bzip2`` decoder plugin decompresses bzip2 files before input plugins read them.

Options
~~~~~~~~

This plugin doesn't have any options.

Example
~~~~~~~~

.. code-block:: yaml

    in:
      ...
      decoders:
      - {type: bzip2}


File output plugin
-------------------

The ``file`` output plugin writes records to local file system.

Options
~~~~~~~~

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
~~~~~~~~

.. code-block:: yaml

    out:
      type: file
      path_prefix: /path/to/output/sample_
      file_ext: csv
      formatter:
        ...

CSV formatter plugin
---------------------

The ``csv`` formatter plugin formats records using CSV or TSV format.

Options
~~~~~~~~

+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| name                 | type    | description                                                                                           | required?                     |
+======================+=========+=======================================================================================================+===============================+
| delimiter            | string  | Delimiter character such as ``,`` for CSV, ``"\t"`` for TSV, ``"|"`` or any single-byte character     | ``,`` by default              |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| quote                | string  | The character surrounding a quoted value                                                              | ``"`` by default              |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| quote\_policy        | enum    | Policy for quote (ALL, MINIMAL, NONE) (see below)                                                     | ``MINIMAL`` by default        |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| escape               | string  | Escape character to escape quote character                                                            | same with quote default (\*1) |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| header\_line         | boolean | If true, write the header line with column name at the first line                                     | ``true`` by default           |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| null_string          | string  | Expression of NULL values                                                                             | empty by default              |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| newline              | enum    | Newline character (CRLF, LF or CR)                                                                    | ``CRLF`` by default           |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| newline\_in\_field   | enum    | Newline character in each field (CRLF, LF, CR)                                                        | ``LF`` by default             |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| charset              | enum    | Character encoding (eg. ISO-8859-1, UTF-8)                                                            | ``UTF-8`` by default          |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| default\_timezone    | string  | Time zone of timestamp columns. This can be overwritten for each column using ``column_options``      | ``UTC`` by default            |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+
| column\_options      | hash    | See bellow                                                                                            | optional                      |
+----------------------+---------+-------------------------------------------------------------------------------------------------------+-------------------------------+

(\*1): if quote\_policy is NONE, ``quote`` option is ignored, and default ``escape`` is ``\``.

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

.. note::

   The Timestamp format refers to `Ruby strftime format <https://docs.ruby-lang.org/en/2.4.0/Date.html#method-i-strftime>`_

Example
~~~~~~~~

.. code-block:: yaml

    out:
      ...
      formatter:
        type: csv
        delimiter: "\t"
        newline: CRLF
        newline_in_field: LF
        charset: UTF-8
        quote_policy: MINIMAL
        quote: '"'
        escape: "\\"
        null_string: "\\N"
        default_timezone: 'UTC'
        column_options:
          mycol1: {format: '%Y-%m-%d %H:%M:%S'}
          mycol2: {format: '%Y-%m-%d %H:%M:%S', timezone: 'America/Los_Angeles'}

Gzip encoder plugin
--------------------

The ``gzip`` encoder plugin compresses output files using gzip.

Options
~~~~~~~~

+---------+----------+----------------------------------------------------------------------+--------------------+
| name    | type     | description                                                          | required?          |
+=========+==========+======================================================================+====================+
| level   | integer  | Compression level. From 0 (no compression) to 9 (best compression).  | ``6`` by default   |
+---------+----------+----------------------------------------------------------------------+--------------------+

Example
~~~~~~~~

.. code-block:: yaml

    out:
      ...
      encoders:
      - type: gzip
        level: 1


BZip2 encoder plugin
---------------------

The ``bzip2`` encoder plugin compresses output files using bzip2.

Options
~~~~~~~~

+---------+----------+----------------------------------------------------------------------+--------------------+
| name    | type     | description                                                          | required?          |
+=========+==========+======================================================================+====================+
| level   | integer  | Compression level. From 1 to 9 (best compression).                   | ``9`` by default   |
+---------+----------+----------------------------------------------------------------------+--------------------+

Example
~~~~~~~~

.. code-block:: yaml

    out:
      ...
      encoders:
      - type: bzip2
        level: 6


Rename filter plugin
---------------------

The ``rename`` filter plugin changes column names. This plugin has no impact on performance.

Options
~~~~~~~~

+---------+----------+----------------------------------------------------------------------+--------------------+
| name    | type     | description                                                          | required?          |
+=========+==========+======================================================================+====================+
| rules   | array    | An array of rule-based renaming operations. (See below for rules.)   | ``[]`` by default  |
+---------+----------+----------------------------------------------------------------------+--------------------+
| columns | hash     | A map whose keys are existing column names. values are new names.    | ``{}`` by default  |
+---------+----------+----------------------------------------------------------------------+--------------------+

Renaming rules
~~~~~~~~~~~~~~~

The ``rules`` is an array of rules as below applied top-down for all the columns.

+-------------------------+----------------------------------------------------------------------------------------+
| rule                    | description                                                                            |
+=========================+========================================================================================+
| character\_types        | Restrict characters by types. Replace restricted characteres.                          |
+-------------------------+----------------------------------------------------------------------------------------+
| first\_character\_types | Restrict the first character by types. Prefix or replace first restricted characters.  |
+-------------------------+----------------------------------------------------------------------------------------+
| lower\_to\_upper        | Convert lower-case alphabets to upper-case.                                            |
+-------------------------+----------------------------------------------------------------------------------------+
| regex\_replace          | Replace with a regular expressions.                                                    |
+-------------------------+----------------------------------------------------------------------------------------+
| truncate                | Truncate.                                                                              |
+-------------------------+----------------------------------------------------------------------------------------+
| upper\_to\_lower        | Convert upper-case alphabets to lower-case                                             |
+-------------------------+----------------------------------------------------------------------------------------+
| unique\_number\_suffix  | Make column names unique in the schema.                                                |
+-------------------------+----------------------------------------------------------------------------------------+

Renaming rule: character\_types
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``character_types`` replaces restricted characters.

+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+--------------------+
| option            | description                                                                                                                                | required?          |
+===================+============================================================================================================================================+====================+
| pass\_characteres | Characters to be allowed.                                                                                                                  | ``""`` by default  |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+--------------------+
| pass\_types       | Sets of characters to be allowed. The array must consist of "a-z" (lower-case alphabets), "A-Z" (upper-case alphabets), or "0-9" (digits). | ``[]`` by default  |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+--------------------+
| replace           | A character that disallowed characters are replaced with. It must consist of just 1 character.                                             | ``"_"`` by default |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+--------------------+

Example
""""""""

.. code-block:: yaml

    # This configuration replaces characters into "_" except for "_", lower-case alphabets, and digits.
    filters:
      ...
      - type: rename
        rules:
        - rule: character_types
          pass_characters: "_"
          pass_types: [ "a-z", "0-9" ]


Renaming rule: first\_character\_types
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``first_character_types`` prefixes or replaces a restricted character at the beginning.

+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+----------------------------------------------+
| option            | description                                                                                                                                | required?                                    |
+===================+============================================================================================================================================+==============================================+
| pass\_characteres | Characters to be allowed.                                                                                                                  | ``""`` by default                            |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+----------------------------------------------+
| pass\_types       | Sets of characters to be allowed. The array must consist of "a-z" (lower-case alphabets), "A-Z" (upper-case alphabets), or "0-9" (digits). | ``[]`` by default                            |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+----------------------------------------------+
| prefix            | A character that a disallowed first character is replaced with.                                                                            | one of ``prefix`` or ``replace`` is required |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+----------------------------------------------+
| replace           | A character that a disallowed first character is prefixed with.                                                                            | one of ``prefix`` or ``replace`` is required |
+-------------------+--------------------------------------------------------------------------------------------------------------------------------------------+----------------------------------------------+

Example
""""""""

.. code-block:: yaml

    # This configuration prefixes a column name with "_" unless the name starts from "_" or a lower-case alphabet.
    filters:
      ...
      - type: rename
        rules:
        - rule: first_character_types
          pass_characters: "_"
          pass_types: [ "a-z" ]
          prefix: "_"

Renaming rule: lower\_to\_upper
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``lower_to_upper`` converts lower-case alphabets to upper-case.

Example
""""""""

.. code-block:: yaml

    # This configuration converts all lower-case alphabets to upper-case.
    filters:
      ...
      - type: rename
        rules:
        - rule: lower_to_upper


Renaming rule: regex\_replace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``regex_replace`` replaces column names based on a regular expression.

+---------+--------------------------------------------------------------------------------------------------------------------------------------+-----------+
| option  | description                                                                                                                          | required? |
+=========+======================================================================================================================================+===========+
| match   | A `Java-style regular expression <https://docs.oracle.com/javase/tutorial/essential/regex/>`_ to which this string is to be matched. | required  |
+---------+--------------------------------------------------------------------------------------------------------------------------------------+-----------+
| replace | A string to be substibuted for each match in Java-style.                                                                             | required  |
+---------+--------------------------------------------------------------------------------------------------------------------------------------+-----------+

Example
""""""""

.. code-block:: yaml

    # This configuration replaces all patterns
    filters:
      ...
      - type: rename
        rules:
        - rule: regex_replace
          match: "([0-9]+)_dollars"
          replace: "USD$1"


Renaming rule: truncate
^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``truncate`` truncates column names.

+------------+-----------------------------------------------------+--------------------+
| option     | description                                         | required?          |
+============+=====================================================+====================+
| max_length | The length to which the column names are truncated. | ``128`` by default |
+------------+-----------------------------------------------------+--------------------+

Example
""""""""

.. code-block:: yaml

    # This configuration drops all characters after the 20th character.
    filters:
      ...
      - type: rename
        rules:
        - rule: truncate
          max_length: 20

Renaming rule: upper\_to\_lower
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``upper_to_lower`` converts upper-case alphabets to lower-case.

Example
""""""""

.. code-block:: yaml

    # This configuration converts all upper-case alphabets to lower-case.
    filters:
      ...
      - type: rename
        rules:
        - rule: upper_to_lower

Renaming rule: unique\_number\_suffix
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The rule ``unique_number_suffix`` makes column names unique in the schema by suffixing numbers.

+------------+-----------------------------------------------------------------------------------------------------------------------------+--------------------+
| option     | description                                                                                                                 | required?          |
+============+=============================================================================================================================+====================+
| delimiter  | A delimiter character inserted before a suffix number. It must be just 1 non-digit character.                               | ``"_"`` by default |
+------------+-----------------------------------------------------------------------------------------------------------------------------+--------------------+
| digits     | An integer that specifies the number of zero-filled digits of a suffix number. The suffix number zero-filled to the digits. | optional           |
+------------+-----------------------------------------------------------------------------------------------------------------------------+--------------------+
| max_length | The length to which the column names are truncated. The column name is truncated before the suffix number.                  | optional           |
+------------+-----------------------------------------------------------------------------------------------------------------------------+--------------------+
| offset     | An integer where the suffix number starts. The first duplicative column name is suffixed by (```offset``` + 1).             | ``1`` by default   |
+------------+-----------------------------------------------------------------------------------------------------------------------------+--------------------+

.. hint::
   The procedure to make column names unique is not very trivial. There are many feasible ways. This renaming rule works as follows:

   Basic policies:

   * Suffix numbers are counted per original column name.
   * Column names are fixed from the first column to the last column.

   Actual procedure applied from the first (leftmost) column to the last (rightmost) column:

   1. Fix the column name as-is with truncating if the truncated name is not duplicated with left columns.
   2. Suffix the column name otherwise.

      a. Try to append the suffix number for the original column name with truncating.
      b. Fix it if the suffixed name is not duplicated with left columns nor original columns.
      c. Retry (a) with the suffix number increased otherwise.

Example
""""""""

.. code-block:: yaml

    # This configuration suffixes numbers to duplicative column names. (Ex. ["column", "column", "column"] goes to ["column", "column_2", "column_3"].)
    filters:
      ...
      - type: rename
        rules:
        - rule: unique_number_suffix

Example of renaming rules
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: yaml

    filters:
      ...
      - type: rename
        rules:
        - rule: upper_to_lower        # All upper-case are converted to lower-case.
        - rule: character_types       # Only lower-case, digits and "_" are allowed. (No upper-case by the rule ahove.)
          pass_types: [ "a-z", "0-9" ]
          pass_characters: "_"
        - rule: unique_number_suffix  # Ensure all column names are unique.

Columns: not recommended
~~~~~~~~~~~~~~~~~~~~~~~~~

``columns`` is not recommended to use anymore. Consider using ``rules`` instead.

.. code-block:: yaml

    filters:
      ...
      - type: rename
        columns:
          my_existing_column1: new_column1
          my_existing_column2: new_column2

.. hint::
   ``columns`` are applied before ``rules`` if ``columns`` and ``rules`` are specified together. (It is discouraged to specify them together, though.)


Remove columns filter plugin
-----------------------------

The ``remove_columns`` filter plugin removes columns from schema.

Options
~~~~~~~~

+--------------------------+----------+------------------------------------------------------------+-----------------------+
| name                     | type     | description                                                | required?             |
+==========================+==========+============================================================+=======================+
| remove                   | array    | An array of names of columns that it removes from schema.  | ``[]`` by default     |
+--------------------------+----------+------------------------------------------------------------+-----------------------+
| keep                     | array    | An array of names of columns that it keeps in schema.      | ``[]`` by default     |
+--------------------------+----------+------------------------------------------------------------+-----------------------+
| accept_unmatched_columns | boolean  | If true, skip columns that aren't included in schemas.     | ``false`` by default  |
+--------------------------+----------+------------------------------------------------------------+-----------------------+


remove: and keep: options are not multi-select.

Example
~~~~~~~~

.. code-block:: yaml

    # This configuration removes "_c0" and "_c1" named columns from schema.
    filters:
      ...
      - type: remove_columns
        remove: ["_c0", "_c1"]


Local executor plugin
----------------------

The ``local`` executor plugin runs tasks using local threads. This is the only built-in executor plugin.

Options
~~~~~~~~

+------------------+----------+----------------------------------------------------------------------+--------------------------------------+
| name             | type     | description                                                          | required?                            |
+==================+==========+======================================================================+======================================+
| max_threads      | integer  | Maximum number of threads to run concurrently.                       | 2x of available CPU cores by default |
+------------------+----------+----------------------------------------------------------------------+--------------------------------------+
| min_output_tasks | integer  | Mimimum number of output tasks to enable page scattering.            | 1x of available CPU cores by default |
+------------------+----------+----------------------------------------------------------------------+--------------------------------------+


The ``max_threads`` option controls maximum concurrency. Setting smaller number here is useful if too many threads make the destination or source storage overloaded. Setting larger number here is useful if CPU utilization is too low due to high latency.

The ``min_output_tasks`` option enables "page scattering". The feature is enabled if number of input tasks is less than ``min_output_tasks``. It uses multiple filter & output threads for each input task so that one input task can use multiple threads. Setting larger number here is useful if embulk doesn't use multi-threading with enough concurrency due to too few number of input tasks. Setting 1 here disables page scattering completely.

Example
~~~~~~~~

.. code-block:: yaml

    exec:
      max_threads: 8         # run at most 8 tasks concurrently
      min_output_tasks: 1    # disable page scattering
    in:
      type: ...
      ...
    out:
      type: ...
      ...

Guess executor
---------------

The guess executor is called by ``guess`` command. It executes default guess plugins in a sequential order and suggests Embulk config by appropriate guess plugin. The default guess plugins and the order are ``gzip``, ``'bzip2``, ``json`` and ``csv``.

Options
~~~~~~~~

+---------------------------+----------+----------------------------------------------------------------------+--------------------------------------+
| name                      | type     | description                                                          | required?                            |
+===========================+==========+======================================================================+======================================+
| guess_plugins             | array    | ``guess`` command uses specified guess plugins.                      | ``[]`` by default                    |
+---------------------------+----------+----------------------------------------------------------------------+--------------------------------------+
| exclude_guess_plugins     | array    | ``guess`` command doesn't use specified plugins.                     | ``[]`` by default                    |
+---------------------------+----------+----------------------------------------------------------------------+--------------------------------------+
| guess_sample_buffer_bytes | int      | Bytes of sample buffer that it tries to read from input source.      | 32768 (32KB) by default              |
+-------------------------------+----------+----------------------------------------------------------------------+----------------------------------+

The ``guess_plugins`` option includes specified guess plugin in the bottom of the list of default guess plugins.

The ``exclude_guess_plugins`` option exclude specified guess plugins from the list of default guess plugins that the guess executor uses.

The ``guess_sample_buffer_bytes`` option controls the bytes of sample buffer that GuessExecutor tries to read from specified input source.

This example shows how to use ``csv_all_strings`` guess plugin, which suggests column types within CSV files as string types. It needs to be explicitly specified by users when it's used instead of ``csv`` guess plugin because the plugin is not included in default guess plugins. We also can exclude default ``csv`` guess plugin.

Example
~~~~~~~~

.. code-block:: yaml

    exec:
      guess_plugins: ['csv_all_strings']
      exclude_guess_plugins: ['csv']
    in:
      type: ...
      ...
    out:
      type: ...
      ...

Preview executor
----------------

The preview executor is called by ``preview`` command. It tries to read sample buffer from a specified input source and writes them to Page objects. ``preview`` outputs the Page objects to console.

Options
~~~~~~~~

+-------------------------------+----------+----------------------------------------------------------------------+--------------------------------------+
| name                          | type     | description                                                          | required?                            |
+===============================+==========+======================================================================+======================================+
| preview_sample_buffer_bytes   | int      | Bytes of sample buffer that it tries to read from input source.      | 32768 (32KB) by default              |
+-------------------------------+----------+----------------------------------------------------------------------+--------------------------------------+

The ``preview_sample_buffer_bytes`` option controls the bytes of sample buffer that PreviewExecutor tries to read from specified input source.

This example shows how to change the bytes of sample buffer.

Example
~~~~~~~~

.. code-block:: yaml

    exec:
      preview_sample_buffer_bytes: 65536 # 64KB
    in:
      type: ...
      ...
    out:
      type: ...
      ...
