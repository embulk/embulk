
module QuickLoad
  require 'quickload/error'
  require 'quickload/plugin_registry'
  require 'quickload/bridge/guess'

  class PluginManager
    PLUGIN_CATEGORIES = %w[
      input output
      parser formatter
      decoder encoder
      line_filter
      guess
    ]

    def initialize
      @registries = {}
      PLUGIN_CATEGORIES.each do |category|
        @registries[category.to_sym] = PluginRegistry.new(category, "quickload/plugin/#{category}_")
      end
    end

    # define
    #   def register_input(type, klass)
    #   def register_output(type, klass)
    #   ...
    #   def new_input(type)
    #   def new_output(type)
    #   ...
    PLUGIN_CATEGORIES.each do |category|
      eval %[
      def register_#{category}(type, klass)
        @registries[:#{category}].register(type, klass)
      end

      def new_#{category}(type)
        klass = @registries[:#{category}].lookup(type)
        plugin = klass.new
        if plugin.is_a?(ParserGuessPlugin)
          Bridge::GuessPluginReverseBridge.new(plugin)
        else
          plugin
        end
      end

      def new_java_#{category}(type)
        klass = @registries[:#{category}].lookup(type)
        plugin = klass.new
        if plugin.is_a?(ParserGuessPlugin)
          plugin
        else
          Bridge::GuessPluginBridge.new(plugin)
        end
      end
      ]
    end

    # TODO new_xxx methods should fallback to Java PluginSource
    # if not found so that ruby plugins can call java plugins.
    # call injector.newPlugin and wrap the instance in a reverse bridge object.
  end

  Plugin = PluginManager.new
end
