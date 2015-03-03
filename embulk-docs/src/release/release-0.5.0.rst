Release 0.5.0
==================================

New Guess Plugin Architecture
------------------

Embulk v0.5.0 supports dynamically loadable guess plugins.

* For parser and decoder plugins:

  * CLI subcommand ``guess`` accepts new ``-g, --guess NAMES`` argument to load parser and decoder guess plugins.

  * Plugin template generator creates stub code of guess plugin for parser and decoder plugins.

* For input plugins:

  * Added ``Embulk::InputPlugin.guess(config)`` and ``spi.InputPlugin#guess(config)`` methods for Ruby and Java plugins.

  * ``guess`` subcommand executes the new guess method and takes the return value of the method.

Plugins can use new ``Guess::SchemaGuess`` utility class for ease of implementation.

For example, if you write a parser plugin named ``myparser``, you can use this configuration file first:

.. code-block:: yaml

    in:
      type: file
      path_prefix: path/to/myfiles
    out:
      type: stdout

The ``embulk guess`` command with ``-g myparser`` argument calls the guess plugin bundled in the plugin:

.. code-block:: console

    $ embulk gem install embulk-parser-myparser
    $ embulk guess config.yml -o guessed.yml -g myparser

On the other hand, if the plugin type is input, you don't need additional command-line arguments. For example, if the input plugin name is ``myinput``, you can use this configuration file:

.. code-block:: yaml

    in:
      type: myinput
    out:
      type: stdout

The ``embulk guess`` command finds the ``InputPlugin.guess`` of the input plugin and calls it:

.. code-block:: console

    $ embulk gem install embulk-input-myinput
    $ embulk guess config.yml -o guessed.yml

Plugin API
------------------

* Added ``Guess::SchemaGuess`` class. This utility class inputs array of hash objects or array of array objects and returns schema.


Plugin SPI
------------------

* Added ``Embulk::InputPlugin.guess(config)`` method for Ruby input plugins.

  * Backward compatibility: existent plugins don't have to implement the method. The default behavior is raising ``NotImplementedError``.

* Added ``spi.InputPlugin#guess(ConfigSource config)`` method for Java input plugins.

  * Backward compatibility: existent plugins don't have to implement the method. Mehtod linkage errors are handled at embulk-core. The default behavior is raising ``UnsupportedOperationException``.

Built-in plugins
------------------

* ``csv`` parser plugin implements ``max_quoted_size_limit`` option. Default value is 131072 (128KB).

  * This option is useful when a column value includes a quote character accidentally. If the plugin detects those values, it skips the line and continues parsing from the next line.

General Changes
------------------

* ``spi.util.FileInputInputStream#skip`` method never returns -1 to follow the Java API standard (@hata++).
* Plugin template generator creates appropriate "Overview" section in README.md file depending on the plugin type.


Release Date
------------------
2015-03-02
