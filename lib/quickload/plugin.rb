
module QuickLoad
  require 'quickload/error'
  require 'quickload/plugin_registry'
  require 'quickload/bridge/guess'

  class PluginManager
    def initialize
      @registries = {}
      %w[input output parser formatter decoder encoder line_filter guess].each do |category|
        @registries[category.to_sym] = PluginRegistry.new(category, "quickload/plugin/#{category}_")
      end
    end


    def register_input(type, klass)
      @registries[:input].register(type, klass)
    end

    def new_input(type)
      new_reverse_bridge(:input, type, InputPlugin, Bridge::InputPluginReverseBridge)
    end

    def new_java_input(type)
      new_bridge(:input, type, InputPlugin, Bridge::InputPluginBridge)
    end


    def register_output(type, klass)
      @registries[:output].register(type, klass)
    end

    def new_output(type)
      new_reverse_bridge(:output, type, OutputPlugin, Bridge::OutputPluginReverseBridge)
    end

    def new_java_output(type)
      new_bridge(:output, type, OutputPlugin, Bridge::OutputPluginBridge)
    end


    def register_parser(type, klass)
      @registries[:parser].register(type, klass)
    end

    def new_parser(type)
      new_reverse_bridge(:parser, type, ParserPlugin, Bridge::ParserPluginReverseBridge)
    end

    def new_java_parser(type)
      new_bridge(:parser, type, ParserPlugin, Bridge::BasicParserPluginBridge)
    end


    def register_formatter(type, klass)
      @registries[:formatter].register(type, klass)
    end

    def new_formatter(type)
      new_reverse_bridge(:formatter, type, FormatterPlugin, Bridge::FormatterPluginReverseBridge)
    end

    def new_java_formatter(type)
      new_bridge(:formatter, type, FormatterPlugin, Bridge::BasicFormatterPluginBridge)
    end


    def register_decoder(type, klass)
      @registries[:decoder].register(type, klass)
    end

    def new_decoder(type)
      new_reverse_bridge(:decoder, type, DecoderPlugin, Bridge::DecoderPluginReverseBridge)
    end

    def new_java_decoder(type)
      new_bridge(:decoder, type, DecoderPlugin, Bridge::DecoderPluginBridge)
    end


    def register_encoder(type, klass)
      @registries[:encoder].register(type, klass)
    end

    def new_encoder(type)
      new_reverse_bridge(:encoder, type, EncoderPlugin, Bridge::EncoderPluginReverseBridge)
    end

    def new_java_encoder(type)
      new_bridge(:encoder, type, EncoderPlugin, Bridge::EncoderPluginBridge)
    end


    def register_guess(type, klass)
      @registries[:guess].register(type, klass)
    end

    def new_guess(type)
      new_reverse_bridge(:guess, type, GuessPlugin, Bridge::GuessPluginReverseBridge)
    end

    def new_java_guess(type)
      new_bridge(:guess, type, GuessPlugin, Bridge::GuessPluginBridge)
    end

    private

    # TODO new_plugin should fallback to Java PluginSource
    # if not found so that ruby plugins can call java plugins.
    # call injector.newPlugin and wrap the instance in a reverse bridge object.

    def new_plugin(category, type)
      @registries[category].lookup(type).new
    end

    def new_bridge(category, type, iface, bridge)
      plugin = new_plugin(category, type)
      if plugin.is_a?(iface)
        plugin
      else
        bridge.new(plugin)
      end
    end

    def new_reverse_bridge(category, type, iface, reverse_bridge)
      plugin = new_plugin(category, type)
      if plugin.is_a?(iface)
        reverse_bridge.new(plugin)
      else
        plugin
      end
    end
  end

  Plugin = PluginManager.new
end
