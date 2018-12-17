SLF4J Logging
==============

Embulk has adopted [SLF4J](https://www.slf4j.org/) for logging both in the core and plugins. This document describes the history and the current status how SLF4J is adopted in Embulk.


Initially
----------

* SLF4J 1.7.9 and Log4j 1.2.17 were chosen in Embulk v0.1.0.
* All slf4j-api, slf4j-log4j12, and log4j had been included in embulk-core's dependencies.
* All slf4j-api, slf4j-log4j12, and log4j had been extracted and included flatly in Embulk's binary executable distributions.
* Plugins depended on slf4j-api in embulk-core's dependencies (as provided) or in executable distributions.
* Logger instances were retrieved from `org.embulk.exec.LoggerProvider` (`com.google.inject.Provider<org.slf4j.ILoggerFactory>`) through Guice.
  * Log4j, the default logger implementation, was initialized/configured in `org.embulk.exec.LoggerProvider`.
  * Plugins had to call `Exec.getLogger`, instead of calling `LoggerFactory.getLogger` directly, so that the plugin could retrieve an overridden Logger.
* Software using Embulk as a library had to override Guice bindings through `EmbulkService` or `EmbulkEmbed` to use another logger implementation.
  * Different logger implementations were able to be assigned to different `ExecSession` instances even in the same Java VM process.
  * Even if `LoggerProvider` was overridden by another logger implementation, slf4j-log4j21 and log4j were still in the classpath.


Since Embulk v0.6.12
--------------------

* Logback 1.1.3 was chosen as a logger implementation in place of Log4j.
* The logger architecture did not change only except for the logger implementation.


Since Embulk v0.9.?? (upcoming)
--------------------------------

* Only slf4j-api will be included in embulk-core's dependencies. Logback is not in the dependencies.
* Both slf4j-api and logback will be extracted and included flatly in Embulk's binary executable distributions.
* Logback will be initialized/configured globally only when the binary executable distribution is executed from CLI.
* Logger instances will be retrieved from `org.slf4j.LoggerFactory` directly, not through Guice.
  * Plugins will be allowed to call `LoggerFactory.getLogger` directly.
  * `Exec.getLogger` and `ExecSession.getLogger` will be deprecated to call `org.slf4j.LoggerFactory#getLogger` directly.
  * The `LoggerProvider` module will be deprecated/removed as well.
* Software using Embulk as a library will need to configure the classpath or classloading to choose the logger implementation.
  * The logger implementation will be chosen globally by SLF4J's default selection method.
  * Only one logger implmentation will be available in the same Java VM process. No more different logger implementations for different `ExecSession` instances.


In the future
--------------

* SLF4J may be updated to the 1.8 series.
* Plugins may have slf4j-api by themselves. Or, different slf4j-api may be assigned to each `PluginClassLoader` by the core.
* The logger implementation may be switched to Embulk's own one to forward to the Reporter Plugin.
