Release 0.8.0
==================================

JSON type
------------------

Embulk v0.8.0 added JSON type support.

* A column with ``json`` type can represent nested values such as maps or arrays. This is useful when both input and output support dynamically-typed values.

* **IMPORTANT**: If input plugin uses JSON type but output plugin is compiled with an old embulk (< 0.8.0), a bulk load transaction fails with a confusing error message. To avoid this issue, please run ``embulk migrate /path/to/embulk-plugin-directory`` to upgrade plugin code, and use the latest plugin. This problem doesn't happen if input plugin doesn't use json type.

* Filter plugins to process JSON types are not ready yet. Expected plugins are for example, flatten a json column into statically-typed columns with guess plugin, extracting a value from a json column using an expression (such as JSONPath) and set it to another column, or building a json column by copying values from other columns.

Page scattering
------------------

Local executor plugin (the default executor) runs multiple tasks even if there is only 1 input task. This improves performance a lot especially if input is a single huge file.

* Its mechanism is that the executor creates 2, 3, 4, or more number of output tasks for each input task. Page chunks from a input task is scattered to output tasks. All of the tasks run in parallel using threads. This feature is called "page scattering".

* Added ``min_output_tasks`` option at ``exec:`` section. Default is 1x of available CPU cores. Page scattering is enabled if number of input tasks is less than this ``min_output_tasks`` option. Setting larger number here is useful if embulk doesn't use multi-threading with enough concurrency due to too few number of input tasks.

* Added ``max_threads`` option at ``exec:`` section. Default is 2x of availalbe CPU cores. This option controls maximum concurrency. Setting smaller number here is useful if too many threads make the destination or source storage overloaded. Setting larger number here is useful if CPU utilization is too low due to high latency.

* The results of output transaction will be deterministic. There're no randomness that depends on timing. However, task assignment changes if ``min_output_tasks`` changes. If you need deterministic results regardless of machines that may have different number of CPU cores, please add ``min_output_tasks`` option to ``exec:`` section. Setting 1 there will disable page scattering completely.

General Changes
------------------

* YAML configuration parser uses stricter rules when it converts type of a non-quoted strings.

  * Strings starting with 0 such as 019 or 0819 will be a string instead of float.

  * Strings looks like a timestamp such as 2015-01-01 will be a string instead of UNIX timestamp.

  * On, Off, Yes, and No (case-insensitive) will be a string instead of boolean. Only true, True, false, False are recognized as a boolean.

* Upraded JRuby version to 9.0.4.0.

Java Plugin API
------------------

* Added ``org.embulk.spi.json.JsonParser`` class to parse a String into an internal representation of a JSON value (org.msgpack.value.Value). Usage of this class is similar to TimestampParser.

* Added ``jsonColumn`` method to ``org.embulk.spi.ColumnVisitor``. At an input plugin or parser plugin, please implement this method using above JsonParser class and ``PageBuilder#stJson`` method. At an output plugin or formatter plugin, you can use ``org.msgpack.value.Value#toJson`` method to convert it to a JSON string.

* Upgraded gradle version to 2.10. ``embulk migrate`` upgrades gradle version of your plugins.

* ``embulk new`` and ``embulk migrate`` adds checkstyle configuration for Java plugins. ``./gradlew checkstyle`` checks code styles.


Run Plugin API
------------------

* ``PageBuilder#add`` accepts a nested object (Hash or Array) when the column type is json.

  * Internally, the value is serialized using MessagePack and passed to Java components through JRuby interface.

* ``page.each`` method will give an nested object (Hash or Array) if the column type is json.


Built-in plugins
------------------

* ``parser-cvs`` and ``guess-csv`` plugins support JSON type. JSON type will be automatically guessed.

* ``formatter-csv`` supports JSON type. JSON column is serialized into a string using JSON format (``Value#toJson()``).

* ``formatter-stdout`` supports JSON type. JSON column is serialized into a string using string representation of Value (``Value#toString()``). This is slightly different from JSON but can represent some superset of json such as non-string keys of objects.

Release Date
------------------
2016-01-13
