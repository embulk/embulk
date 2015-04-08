module Embulk

  require 'embulk/data_source'

  class ExecutorPlugin
    # TODO

    if Embulk.java?
      # TODO new_java

      def self.from_java(java_class)
        JavaPlugin.ruby_adapter_class(java_class, ExecutorPlugin, RubyAdapter)
      end

      module RubyAdapter
        module ClassMethods
        end
        # TODO
      end
    end
  end

end
