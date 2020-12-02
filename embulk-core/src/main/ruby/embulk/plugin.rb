
module Embulk
  require 'forwardable'
  require 'embulk/error'
  require 'embulk/plugin_registry'
  require 'embulk/input_plugin'
  require 'embulk/file_input_plugin'
  require 'embulk/output_plugin'
  require 'embulk/file_output_plugin'
  require 'embulk/filter_plugin'
  require 'embulk/parser_plugin'
  require 'embulk/formatter_plugin'
  require 'embulk/decoder_plugin'
  require 'embulk/encoder_plugin'
  require 'embulk/guess_plugin'
  require 'embulk/executor_plugin'
  require 'embulk/java_plugin'
  require 'embulk/exec'

  class PluginManager
    def initialize
      @registries = {}
      %w[input output parser formatter decoder encoder line_filter filter guess executor].each do |category|
        @registries[category.to_sym] = PluginRegistry.new(category, "embulk/#{category}/")
      end
    end

    def register_input(type, klass)
      register_plugin(:input, type, klass, InputPlugin)
    end

    def register_output(type, klass)
      register_plugin(:output, type, klass, OutputPlugin,
                      "Output plugin #{klass} must extend OutputPlugin")
    end

    def register_filter(type, klass)
      register_plugin(:filter, type, klass, FilterPlugin)
    end

    def register_parser(type, klass)
      register_plugin(:parser, type, klass, ParserPlugin)
    end

    def register_formatter(type, klass)
      register_plugin(:formatter, type, klass, FormatterPlugin)
    end

    ## TODO DecoderPlugin JRuby API is not written by anyone yet
    #def register_decoder(type, klass)
    #  register_plugin(:decoder, type, klass, DecoderPlugin)
    #end

    ## TODO EncoderPlugin JRuby API is not written by anyone yet
    #def register_encoder(type, klass)
    #  register_plugin(:encoder, type, klass, EncoderPlugin)
    #end

    def register_guess(type, klass)
      register_plugin(:guess, type, klass, GuessPlugin,
                     "Guess plugin #{klass} must extend GuessPlugin, LineGuessPlugin, or TextGuessPlugin class")
    end

    # TODO InputPlugin::RubyAdapter is not written by anyone yet
    #def get_input(type)
    #  lookup(:input, type)
    #end

    # TODO OutputPlugin::RubyAdapter is not written by anyone yet
    #def get_output(type)
    #  lookup(:output, type)
    #end

    # TODO FilterPlugin::RubyAdapter is not written by anyone yet
    #def get_filter(type)
    #  lookup(:filter, type)
    #end

    # TODO FilterPlugin::RubyAdapter is not written by anyone yet
    #def get_parser(type)
    #  # TODO not implemented yet
    #  lookup(:parser, type)
    #end

    # TODO FormatterPlugin::RubyAdapter is not written by anyone yet
    #def get_formatter(type)
    #  # TODO not implemented yet
    #  lookup(:formatter, type)
    #end

    # TODO DecoderPlugin::RubyAdapter is not written by anyone yet
    #def get_decoder(type)
    #  # TODO not implemented yet
    #  lookup(:decoder, type)
    #end

    # TODO EncoderPlugin::RubyAdapter is not written by anyone yet
    #def get_encoder(type)
    #  # TODO not implemented yet
    #  lookup(:encoder, type)
    #end

    def get_guess(type)
      lookup(:guess, type)
    end

    def register_java_input(type, klass)
      register_java_plugin(:input, type, klass,
                           "org.embulk.spi.InputPlugin" => InputPlugin,
                           "org.embulk.spi.FileInputPlugin" => FileInputPlugin)
    end

    def register_java_output(type, klass)
      register_java_plugin(:output, type, klass,
                           "org.embulk.spi.OutputPlugin" => OutputPlugin,
                           "org.embulk.spi.FileOutputPlugin" => FileOutputPlugin)
    end

    def register_java_filter(type, klass)
      register_java_plugin(:filter, type, klass,
                           "org.embulk.spi.FilterPlugin" => FilterPlugin)
    end

    def register_java_parser(type, klass)
      register_java_plugin(:parser, type, klass,
                           "org.embulk.spi.ParserPlugin" => ParserPlugin)
    end

    def register_java_formatter(type, klass)
      register_java_plugin(:formatter, type, klass,
                           "org.embulk.spi.FormatterPlugin" => FormatterPlugin)
    end

    def register_java_decoder(type, klass)
      register_java_plugin(:decoder, type, klass,
                           "org.embulk.spi.DecoderPlugin" => DecoderPlugin)
    end

    def register_java_encoder(type, klass)
      register_java_plugin(:encoder, type, klass,
                           "org.embulk.spi.EncoderPlugin" => EncoderPlugin)
    end

    def register_java_guess(type, klass)
      register_java_plugin(:guess, type, klass,
                           "org.embulk.spi.GuessPlugin" => GuessPlugin)
    end

    def register_java_executor(type, klass)
      register_java_plugin(:executor, type, klass,
                           "org.embulk.spi.ExecutorPlugin" => ExecutorPlugin)
    end

    def new_java_input(type)
      lookup(:input, type).new_java
    end

    def new_java_output(type)
      lookup(:output, type).new_java
    end

    def new_java_filter(type)
      lookup(:filter, type).new_java
    end

    def new_java_parser(type)
      lookup(:parser, type).new_java
    end

    def new_java_formatter(type)
      lookup(:formatter, type).new_java
    end

    def new_java_decoder(type)
      lookup(:decoder, type).new_java
    end

    def new_java_encoder(type)
      lookup(:encoder, type).new_java
    end

    def new_java_guess(type)
      lookup(:guess, type).new_java
    end

    def new_java_executor(type)
      lookup(:executor, type).new_java
    end

    private

    # TODO lookup should fallback to Java PluginSource
    # if not found so that ruby plugins can call java plugins.
    # call Java.injector.newPlugin and wrap the instance in a reverse bridge object.

    def lookup(category, type)
      @registries[category].lookup(type)
    end

    def register_plugin(category, type, klass, base_class, message=nil)
      unless klass < base_class
        message ||= "Plugin #{klass} must inherit #{base_class}"
        raise message
      end
      @registries[category].register(type, klass)
    end

    def register_java_plugin(category, type, klass, iface_map, message=nil)
      found = iface_map.find do |iface_name,ruby_base_class|
        iface = JRuby.runtime.getJRubyClassLoader.load_class(iface_name)
        iface.isAssignableFrom(klass)
      end
      unless found
        message ||= "Java plugin #{klass} must implement #{iface_map.keys.join(' or ')}"
        raise message
      end
      adapted = found.last.from_java(klass)
      @registries[category].register(type, adapted)
    end
  end

  module Plugin
    INSTANCE = PluginManager.new
    class <<self

      extend Forwardable

      def_delegators :'Embulk::Plugin::INSTANCE',
        :register_input, :get_input, :register_java_input, :new_java_input,
        :register_output, :get_output, :register_java_output, :new_java_output,
        :register_filter, :get_filter, :register_java_filter, :new_java_filter,
        :register_parser, :get_parser, :register_java_parser, :new_java_parser,
        :register_formatter, :get_formatter, :register_java_formatter, :new_java_formatter,
        :register_decoder, :get_decoder, :register_java_decoder, :new_java_decoder,
        :register_encoder, :get_encoder, :register_java_encoder, :new_java_encoder,
        :register_guess, :get_guess, :register_java_guess, :new_java_guess,
        :register_executor, :get_executor, :register_java_executor, :new_java_executor
    end
  end
end
