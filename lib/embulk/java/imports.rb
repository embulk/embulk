require 'java'

#
# this file is loaded by embulk/java/bootstrap.rb
#

module Embulk::Java
  java_import 'org.embulk.spi.Buffer'

  java_import 'org.embulk.config.DataSourceImpl'
  java_import 'org.embulk.spi.GuessPlugin'
  java_import 'org.embulk.spi.OutputPlugin'
  java_import 'org.embulk.spi.InputPlugin'
  java_import 'org.embulk.spi.TransactionalPageOutput'

  java_import 'org.embulk.spi.LineDecoder'
  java_import 'org.embulk.spi.ListFileInput'
  java_import 'org.embulk.spi.PageReader'
  java_import 'org.embulk.spi.PageBuilder'

  java_import 'org.embulk.type.Schema'
  java_import 'org.embulk.type.Column'
  java_import 'org.embulk.type.Type'
  java_import 'org.embulk.type.Types'

  # TODO
end
