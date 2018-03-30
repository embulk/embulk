module Embulk

  require 'embulk/data_source'

  class ReporterPlugin
    def self.from_java(java_class)
      JavaPlugin.ruby_adapter_class(java_class, ReporterPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
      end
      # TODO
    end
  end

end
