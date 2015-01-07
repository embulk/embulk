require 'java'

#
# this file is loaded by embulk/java/bootstrap.rb
#

module Embulk::Java
  java_import 'org.embulk.spi.Buffer'

  java_import 'org.embulk.config.DataSourceImpl'
  java_import 'org.embulk.spi.GuessPlugin'

  java_import 'org.embulk.spi.LineDecoder'
  java_import 'org.embulk.spi.ListFileInput'

  # TODO
end
