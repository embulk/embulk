
module Embulk
  require 'forwardable'
  require 'embulk/error'
  require 'embulk/plugin_registry'
  require 'embulk/input_plugin'
  require 'embulk/filter_plugin'
  require 'embulk/output_plugin'
  #require 'embulk/parser_plugin'
  #require 'embulk/formatter_plugin'
  #require 'embulk/decoder_plugin'
  #require 'embulk/encoder_plugin'
  require 'embulk/guess_plugin'

  class PluginManager
    def initialize
      @registries = {}
      %w[input output parser formatter decoder encoder line_filter filter guess].each do |category|
        @registries[category.to_sym] = PluginRegistry.new(category, "embulk/#{category}_")
      end
    end

    def register_input(type, klass)
      register_plugin(:input, type, klass, InputPlugin)
    end

    def register_output(type, klass)
      register_plugin(:output, type, klass, OutputPlugin,
                      "Output plugin #{klass} must extend OutputPlugin")
    end

    def register_parser(type, klass)
      register_plugin(:parser, type, klass, ParserPlugin)
    end

    def register_formatter(type, klass)
      register_plugin(:formatter, type, klass, FormatterPlugin)
    end

    def register_decoder(type, klass)
      register_plugin(:decoder, type, klass, DecoderPlugin)
    end

    def register_encoder(type, klass)
      register_plugin(:encoder, type, klass, EncoderPlugin)
    end

    def register_filter(type, klass)
      register_plugin(:filter, type, klass, FilterPlugin)
    end

    def register_guess(type, klass)
      register_plugin(:guess, type, klass, GuessPlugin,
                     "Guess plugin #{klass} must extend GuessPlugin, LineGuessPlugin, or TextGuessPlugin class")
    end

    def get_input(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_output(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_parser(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_formatter(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_decoder(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_encoder(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_filter(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def get_guess(type)
      # TODO not implemented yet
      lookup(:guess, type)
    end

    def new_java_input(type)
      lookup(:input, type).java_object
    end

    def new_java_output(type)
      lookup(:output, type).java_object
    end

    def new_java_parser(type)
      lookup(:parser, type).java_object
    end

    def new_java_formatter(type)
      lookup(:formatter, type).java_object
    end

    def new_java_decoder(type)
      lookup(:decoder, type).java_object
    end

    def new_java_encoder(type)
      lookup(:encoder, type).java_object
    end

    def new_java_filter(type)
      lookup(:filter, type).java_object
    end

    def new_java_guess(type)
      lookup(:guess, type).java_object
    end

    private

    # TODO lookup should fallback to Java PluginSource
    # if not found so that ruby plugins can call java plugins.
    # call injector.newPlugin and wrap the instance in a reverse bridge object.

    def lookup(category, type)
      @registries[category].lookup(type)
    end

    def register_plugin(category, type, klass, iface, message=nil)
      unless klass < iface
        message ||= "Plugin #{klass} must implement #{iface}"
        raise message
      end
      @registries[category].register(type, klass)
    end
  end

  module Plugin
    class <<self
      INSTANCE = PluginManager.new

      extend Forwardable

      def_delegators 'INSTANCE',
        :register_input, :get_input, :new_java_input,
        :register_output, :get_output, :new_java_output,
        :register_parser, :get_parser, :new_java_parser,
        :register_formatter, :get_formatter, :new_java_formatter,
        :register_decoder, :get_decoder, :new_java_decoder,
        :register_encoder, :get_encoder, :new_java_encoder,
        :register_filter, :get_filter, :new_java_filter,
        :register_guess, :get_guess, :new_java_guess
    end
  end
end
