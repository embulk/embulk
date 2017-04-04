Customization
==================================

.. contents::
   :local:
   :depth: 2


Creating plugins
------------------

Creating a new plugin is 4 steps:

1. Create a new project using a template generator
2. Build the project (Java only)
3. Confirm it works
4. Modify the code as you need

This article describes how to create a plugin step by step.

Step 1: Creating a new project
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Embulk comes with a number of templates that generates a new project so that you can start development instantly. Because the generated project contains completely working code without additional code, you can focus on the necessary coding.

Usage of the generator is where ``<type>`` is one of the plugin templates:

::

    $ embulk new <type> <name>

Here is the list of available templates. Please find the best option:

====================  ===============================  =================
Type                  description                      example
====================  ===============================  =================
**java-input**        Java record input plugin         ``mysql``
**java-output**       Java record output plugin        ``mysql``
**java-filter**       Java record filter plugin        ``add-hostname``
**java-file-input**   Java file input plugin           ``ftp``
**java-file-output**  Java file output plugin          ``ftp``
**java-parser**       Java file parser plugin          ``csv``
**java-formatter**    Java file formatter plugin       ``csv``
**java-decoder**      Java file decoder plugin         ``gzip``
**java-encoder**      Java file encoder plugin         ``gzip``
**ruby-input**        Ruby record input plugin         ``mysql``
**ruby-output**       Ruby record output plugin        ``mysql``
**ruby-filter**       Ruby record filter plugin        ``add-hostname``
**ruby-parser**       Ruby file parser plugin          ``csv``
**ruby-formatter**    Ruby file formatter plugin       ``csv``
====================  ===============================  =================

For example, if you want to parse a new file format using Java, type this command:

::

    $ embulk new java-parser myformat

This will create a Java-based parser plugin called ``myformat`` in ``embulk-parser-myformat`` directory.

Step 2: Building the project
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If the plugin is Java-based, you need to build the project. To build, type this command:

::

    $ cd embulk-parser-myformat
    $ ./gradlew package

Now, the plugin is ready to use.

Step 3: Confirm it works
~~~~~~~~~~~~~~~~~~~~~~~~

The next step is to actually use the plugin.

Let's suppose you have a configuration file named ``your-config.yml``. You can use the plugin using embulk with ``-L`` argument:

::

    $ embulk run -L ./embulk-parser-myformat/ your-config.yml

Step 4: Modifying the code
~~~~~~~~~~~~~~~~~~~~~~~~~~

The final step is to modify code as you want!

The code is located at

* Java-based plugins

  * src/org/embulk/*

* Ruby-based plugins

  * lib/embulk/*

There are a lot of good code examples on Github. Search repositories by `embulk-<type> keyword <https://github.com/search?q=embulk-output>`_.

Releasing plugins
------------------

You can release publicly so that all people can use your awesome plugins.

Checking plugin description
~~~~~~~~~~~~~~~~~~~~~~~~~~~

To prepare the plugin ready to release, you need to include some additional information. The plugin information is written in this file:

* Java-based plugins

  * ``build.gradle`` file

* Ruby-based plugins

  * ``embulk-<type>-<name>.gemspec`` file (``<type>`` is plugin type and ``<name>`` is plugin name)

You will find following section in the file.

.. code-block:: ruby

    Gem::Specification.new do |spec|
        # ...

        spec.authors       = ["Your Name"]
        spec.summary       = %[Myformat parser plugin for Embulk]
        spec.description   = %[Parses Myformat files read by other file input plugins.]
        spec.email         = ["you@example.org"]
        spec.licenses      = ["MIT"]
        spec.homepage      = "https://github.com/frsyuki/embulk-parser-myformat"

        # ...
    end

The items in above example are important. Please make sure that they are good.

Creating account on RubyGems.org
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Embulk uses `RubyGems.org <https://rubygems.org/>`_ as a package distribution service. Please create an account there to release plugins at `Sign Up <https://rubygems.org/sign_up>`_ page.

Don't forget the password! It will be necessary at the next step.

Releasing the plugin to RubyGems.org
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now, you're ready to release the plugin. To release, type following command:

* Java-based plugins

  * ``$ ./gradlew gemPush``

* Ruby-based plugins

  * ``$ rake release``

If everything is good, you can find your plugin at https://rubygems.org/. Congratulations!

Installing your plugin
~~~~~~~~~~~~~~~~~~~~~~

Usage of plugin installer is:

::

    $ embulk gem install embulk-<type>-<name>

``<type>`` is plugin type and ``<name>`` is plugin name.

If your plugin is ``embulk-parser-myformat``, then type this command:

::

    $ embulk gem install embulk-parser-myformat

This command installs the plugin to ``~/.embulk`` directory.

To check the list of installed plugins and their versions, use this command:

::

    $ embulk gem list

