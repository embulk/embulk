require 'java'

module Embulk::Java
  java_import 'org.embulk.spi.Buffer'
  java_import 'org.embulk.jruby.BufferBridge'

  java_import 'org.embulk.config.DataSourceImpl'
  java_import 'org.embulk.jruby.DataSourceBridge'
  java_import 'org.embulk.spi.GuessPlugin'

  java_import 'org.embulk.spi.LineDecoder'
  java_import 'org.embulk.spi.ListFileInput'

  # TODO
end
