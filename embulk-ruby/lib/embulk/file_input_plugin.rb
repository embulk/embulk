module Embulk

  class FileInputPlugin
    # TODO transaction, resume, cleanup
    # TODO run

    # TODO to_java

    def self.from_java(java_class)
      JavaPlugin.ruby_adapter_class(java_class, FileInputPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
        def new_java
          Java::FileInputRunner.new(Java::org.embulk.plugin.PluginManager.newPluginInstance(plugin_java_class, Embulk::Java::EmbulkSystemProperties), Embulk::Java::EmbulkSystemProperties)
        end
        # TODO transaction, resume, cleanup
      end

      # TODO run
    end
  end

end
