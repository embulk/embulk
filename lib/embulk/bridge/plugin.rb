module Embulk
  module Bridge
    module Plugin
      # This method initializes ruby_bridge and java_bridge to register plugins to for Java runtime
      def self.initialize_bridge(category, ruby_bridge, java_bridge)
        prepare_java_bridge(java_bridge)
        prepare_ruby_bridge(category, ruby_bridge, java_bridge)
      end

      # TODO private
      def self.prepare_java_bridge(bridge)
        include_module(bridge, JavaPluginBridgeModule)
      end

      def self.prepare_ruby_bridge(category, ruby_bridge, java_bridge)
        include_module(ruby_bridge, ruby_bridge_hook_module(category, ruby_bridge, java_bridge))
      end

      # Java side bridge sets @ruby based on @@ruby_class on initialize
      module JavaPluginBridgeModule
        def self.included(klass)
          klass.class_eval {
            def initialize
              @ruby = self.class.class_variable_get(:@@ruby_class).new
            end
          }
        end
      end

      # Ruby side bridge generates an anonymous class with @@ruby_class and register it as a plugin
      def self.ruby_bridge_hook_module(category, ruby_bridge, java_bridge)
        Module.new {
          @category, @java_bridge = category, java_bridge # captured from def scope
          def self.included(hooked)
            category, java_bridge = @category, @java_bridge # captured from class_eval scope
            hooked.class_eval {
              @category, @java_bridge = category, java_bridge # captured from def scope
              def self.inherited(klass)
                name = klass.name.split('::').last.downcase
                bridge_class = Class.new(@java_bridge)
                bridge_class.class_variable_set(:@@ruby_class, klass)
                # TODO simplify PluginRegistry methods
                Embulk::Plugin.send("register_#{@category}", name, bridge_class)
              end
            }
          end
        }
      end

      def self.include_module(klass, include_module)
        klass.class_eval {
          include include_module
        }
      end
    end
  end
end
