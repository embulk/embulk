# Required to use "Java features" from the Ruby side.
require 'java'

# Required to access JRuby-specific internal features, such as `JRuby.runtime`.
require 'jruby' if Gem::Version.new(JRUBY_VERSION) >= Gem::Version.new('9.3')

# Both of 'java' and 'jruby' should be loaded in Embulk's use-case in nature.
#
# Embulk had loaded only 'java' explicitly until Embulk v0.10.35.
# It was because loading 'java' automatically loaded also 'jruby' until JRuby 9.2.
# But, it has changed since JRuby 9.3. JRuby 9.3+ needs loading 'jruby' explicitly.
#
# See also: https://github.com/jruby/jruby/issues/7221#issuecomment-1133646241
#
# Here, Embulk limits loading 'jruby' only in JRuby 9.3+ to ensure compatibility.
# We may remove the limitation in the future.

#
# this file is loaded by embulk/java/bootstrap.rb
#

module Embulk::Java
  module Config
    include_package 'org.embulk.config'
  end

  module Exec
    include_package 'org.embulk.exec'
  end

  module Plugin
    include_package 'org.embulk.plugin'
  end

  module SPI
    module Time
      include_package 'org.embulk.spi.time'
    end

    module Json
      include_package 'org.embulk.spi.json'
    end

    module Type
      include_package 'org.embulk.spi.type'
    end

    module Unit
      include_package 'org.embulk.spi.unit'
    end

    module Util
      include_package 'org.embulk.spi.util'
    end

    include_package 'org.embulk.spi'
  end

  include_package 'org.embulk'

  java_import 'org.embulk.spi.Buffer'
  java_import 'org.embulk.spi.time.Timestamp'
  java_import 'org.embulk.spi.time.TimestampParseException'
  java_import 'org.embulk.spi.GuessPlugin'
  java_import 'org.embulk.spi.OutputPlugin'
  java_import 'org.embulk.spi.FilterPlugin'
  java_import 'org.embulk.spi.InputPlugin'
  java_import 'org.embulk.spi.ParserPlugin'
  java_import 'org.embulk.spi.FormatterPlugin'
  java_import 'org.embulk.spi.EncoderPlugin'
  java_import 'org.embulk.spi.DecoderPlugin'
  java_import 'org.embulk.spi.TransactionalPageOutput'
  java_import 'org.embulk.spi.PageReader'
  java_import 'org.embulk.spi.PageBuilder'
  java_import 'org.embulk.spi.util.DynamicPageBuilder'
  java_import 'org.embulk.spi.util.LineDecoder'
  java_import 'org.embulk.spi.util.ListFileInput'
  java_import 'org.embulk.spi.Schema'
  java_import 'org.embulk.spi.Column'
  java_import 'org.embulk.spi.type.Type'
  java_import 'org.embulk.spi.type.Types'
  java_import 'org.embulk.spi.FileInputRunner'
  java_import 'org.embulk.spi.FileOutputRunner'
  java_import 'org.embulk.plugin.PluginClassLoaderFactory'

  # TODO
end
