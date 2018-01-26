# Deprecations

## Goal of this document

Embulk has some deprecated classes and methods. This document is to summarize deprecations with their reasons and plans to remove.

## List of deprecations

### `org.embulk.EmbulkService`

`org.embulk.EmbulkEmbed` has replaced `EmbulkService`.

`EmbulkService` is to be removed by v0.10 or earlier. Note that `EmbulkService` is constructed only in the Embulk core or user's code embedding Embulk. `EmbulkService` is not to be constructed from plugins.

* `org.embulk.EmbulkService`

### `org.embulk.config.CommitReport`

`org.embulk.config.TaskReport` has replaced `CommitReport`. `CommitReport` has been deprecated since v0.6.24.

`org.embulk.config.CommitReport` is to be removed by v0.10 or earlier.

* `org.embulk.config.CommitReport`
* `org.embulk.exec.ResumeState#getInputCommitReports()`
* `org.embulk.exec.ResumeState#getOutputCommitReports()`
* `org.embulk.spi.Exec#newCommitReport()`
* `org.embulk.spi.ExecSession#newCommitReport()`
* `org.embulk.spi.TaskState#getCommitReport()`
* `org.embulk.spi.TaskState#setCommitReport()`

### `org.embulk.config.ConfigLoader.fromJson(JsonParser)`

`fromJson(InputStream)` has replaced `fromJson(JsonParser)`. `fromJson(JsonParser)` has been deprecated since v0.6.18.

`fromJson(JsonParser)` is to be removed by v0.10 or earlier.

* `org.embulk.config.ConfigLoader.fromJson(JsonParser)`

### `org.embulk.plugin.PluginClassLoader(Collection<URL>, ClassLoader, Collection<String>, Collection<String>)`

`PluginClassLoader` should be constructed through its static creator methods. Note that `PluginClassLoader` is not to be constructed from plugins.

The constructor is to be removed by v0.10 or earlier.

* `org.embulk.plugin.PluginClassLoader(Collection<URL>, ClassLoader, Collection<String>, Collection<String>)`

### `format` in `org.embulk.spi.ColumnConfig`

`format` for `ColumnConfig` should be provided through `org.embulk.config.ConfigSource`, not by `String format` directly. They have been deprecated since v0.6.14.

They are to be removed by v0.10 or earlier.

* `org.embulk.spi.ColumnConfig(String, org.embulk.spi.type.Type, String)`
* `org.embulk.spi.ColumnConfig#getFormat`

### `org.embulk.spi.ExecSession.SessionTask`

`ExecSession.SessionTask` has been deprecated since v0.6.16.

It is to be removed by v0.10 or earlier.

* `org.embulk.spi.ExecSession.SessionTask`

### `org.embulk.spi.ExecSession(com.google.inject.Injector, org.embulk.config.ConfigSource)`

`ExecSession` should be constructed through `ExecSession.Builder`, not directly by the constructor.

The constructor is to be removed by v0.10 or earlier. Note that `ExecSession` is to be constructed only in the Embulk core, not to be constructed from plugins.

* `org.embulk.spi.ExecSession(com.google.inject.Injector, org.embulk.config.ConfigSource)`

### `org.embulk.spi.time.TimestampFormat`

`TimestampFormat` has been just a wrapper of String. Only `DynamicPageBuilder.ColumnOption#getTimestampFormat()` has used it, but `getTimestampFormat` has been deprecated as well. Its static method `parseDateTimeZone(String)` has been used from some other classes, but that has moved to `org.embulk.spi.time.TimeZoneIds`.

`TimestampFormat` is to be removed eventually, but kept at least until v0.10.

* `org.embulk.spi.time.TimestampFormat`
* `org.embulk.spi.util.DynamicPageBuilder.ColumnOption#getTimestampFormat()`

### All `public` constructors of `org.embulk.spi.time.TimestampFormatter`

`TimestampFormatter` should be constructed through its static creator methods `of`.

The constructors are to be removed eventually, but kept at least until v0.10.

* `org.embulk.spi.time.TimestampFormatter(TimestampFormatter.Task, com.google.common.base.Optional)`
* `org.embulk.spi.time.TimestampFormatter(String, org.joda.time.DateTimeZone)`

### `org.embulk.spi.time.TimestampFormatter#format(org.embulk.spi.time.Timestamp, org.embulk.spi.util.LineEncoder)`

`format` with `LineEncoder` is just a wrapper. It should be removed to reduce unnecessary dependencies.

It is to be removed by v0.10 or earlier.

* `org.embulk.spi.time.TimestampFormatter#format(org.embulk.spi.time.Timestamp, org.embulk.spi.util.LineEncoder)`

### All `public` constructors of `org.embulk.spi.time.TimestampParser`

`TimestampParser` should be constructed through its `of` static methods.

The constructors are to be removed eventually, but kept at least until v0.10.

* `org.embulk.spi.time.TimestampParser(TimestampParser.Task, TimestampParser.TimestampColumnOption)`
* `org.embulk.spi.time.TimestampParser(String, org.joda.time.DateTimeZone)`
* `org.embulk.spi.time.TimestampParser(String, org.joda.time.DateTimeZone, String)`

### `org.embulk.spi.time.TimestampParser.of(String, String, String)`

The third `String` represents `default_date` which has been deprecated.

The method is to be removed eventually, but kept at least until v0.10.

* `org.embulk.spi.time.TimestampParser.of(String, String, String)`

### Usage of `org.joda.time.DateTimeZone`

`org.joda.time.DateTimeZone` should not be used to represent time zones.

They are to be removed eventually, but kept at least until v0.10.

* `org.embulk.spi.ExecSession#newTimestampFormatter()`
* `org.embulk.spi.time.TimestampFormatter#getTimeZone()`
* `org.embulk.spi.time.TimestampFormatter.Task#getDefaultTimeZone()`
* `org.embulk.spi.time.TimestampFormatter.TimestampColumnOption#getTimeZone()`
* `org.embulk.spi.time.TimestampParser#getDefaultTimeZone()`
* `org.embulk.spi.time.TimestampParser.Task#getDefaultTimeZone()`
* `org.embulk.spi.time.TimestampParser.TimestampColumnOption#getTimeZone()`
* `org.embulk.spi.util.DynamicPageBuilder.BuilderTask#getDefaultTimeZone()`
* `org.embulk.spi.util.DynamicPageBuilder.ColumnOption.getTimeZone()`
* `org.embulk.spi.util.PagePrinter(org.embulk.spi.Schema, org.joda.time.DateTimeZone)`
* `org.embulk.standards.StdoutOutputPlugin.PluginTask#getTimeZone()`

### `format` in `org.embulk.spi.type.TimestampType`

`format` should not be contained in `TimestampType`.

They are to be removed eventually, but kept at least until v0.10.

* `org.embulk.spi.type.TimestampType#getFormat()`
* `org.embulk.spi.type.TimestampType#withFormat(String)`
