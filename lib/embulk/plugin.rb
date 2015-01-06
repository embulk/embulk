
module Embulk
  require 'forwardable'
  require 'embulk/error'
  require 'embulk/plugin_registry'
  require 'embulk/plugin/guess_plugin'

  #require 'embulk/plugin/input'
  #require 'embulk/plugin/parser'
  #require 'embulk/plugin/guess'

  class PluginManager
    def initialize
      @registries = {}
      %w[input output parser formatter decoder encoder line_filter guess].each do |category|
        @registries[category.to_sym] = PluginRegistry.new(category, "embulk/plugin/#{category}_")
      end
    end

    def register_input(type, klass)
      register_plugin(:input, type, klass, Java::InputPlugin)
    end

    def register_output(type, klass)
      register_plugin(:output, type, klass, Java::OutputPlugin)
    end

    def register_parser(type, klass)
      register_plugin(:parser, type, klass, Java::ParserPlugin)
    end

    def register_formatter(type, klass)
      register_plugin(:formatter, type, klass, Java::FormatterPlugin)
    end

    def register_decoder(type, klass)
      register_plugin(:decoder, type, klass, Java::DecoderPlugin)
    end

    def register_encoder(type, klass)
      register_plugin(:encoder, type, klass, Java::EncoderPlugin)
    end

    def register_guess(type, klass)
      register_plugin(:guess, type, klass, Java::GuessPlugin,
                     "Guess plugin #{klass} must inherit Guess, LineGuess, or TextGuess class")
    end

    def new_input(type)
      raise NotImplementedError, "Reverse bridge for Ruby of Java input plugins is not implemented yet"
    end

    def new_output(type)
      raise NotImplementedError, "Reverse bridge for Ruby of Java output plugins is not implemented yet"
    end

    def new_parser(type)
      raise NotImplementedError, "Reverse bridge for Ruby of Java parser plugins is not implemented yet"
    end

    def new_formatter(type)
      raise NotImplementedError, "Reverse bridge for Ruby of Java formatter plugins is not implemented yet"
    end

    def new_decoder(type)
      raise NotImplementedError, "Reverse bridge for Ruby of Java decoder plugins is not implemented yet"
    end

    def new_encoder(type)
      raise NotImplementedError, "Reverse bridge for Ruby of Java encoder plugins is not implemented yet"
    end

    def new_guess(type)
      plugin = new_plugin(:guess, type)
      #unless plugin.respond_to?(:guess_buffer)
      #  plugin.extend(Bridge::GuessPluginReverseBridge)
      #end
      plugin
    end

    def new_java_input(type)
      new_plugin(:input, type)
    end

    def new_java_output(type)
      new_plugin(:output, type)
    end

    def new_java_parser(type)
      new_plugin(:parser, type)
    end

    def new_java_formatter(type)
      new_plugin(:formatter, type)
    end

    def new_java_decoder(type)
      new_plugin(:decoder, type)
    end

    def new_java_encoder(type)
      new_plugin(:encoder, type)
    end

    def new_java_guess(type)
      new_plugin(:guess, type)
    end

    private

    # TODO new_plugin should fallback to Java PluginSource
    # if not found so that ruby plugins can call java plugins.
    # call injector.newPlugin and wrap the instance in a reverse bridge object.

    def new_plugin(category, type)
      @registries[category].lookup(type).new
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
        :register_input, :new_input, :new_java_input,
        :register_output, :new_output, :new_java_output,
        :register_parser, :new_parser, :new_java_parser,
        :register_formatter, :new_formatter, :new_java_formatter,
        :register_decoder, :new_decoder, :new_java_decoder,
        :register_encoder, :new_encoder, :new_java_encoder,
        :register_guess, :new_guess, :new_java_guess
    end

    # Embulk::Plugin::Plugin
    Plugin = self
  end
end
