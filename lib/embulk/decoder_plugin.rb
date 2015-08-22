module Embulk

  require 'embulk/data_source'

  class DecoderPlugin
    def self.transaction(config, &control)
      raise NotImplementedError, "DecoderPlugin.transaction(config, &control) must be implemented"
    end

    # TODO

    # TODO new_java

    def self.from_java(java_class)
      JavaPlugin.ruby_adapter_class(java_class, DecoderPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
      end
      # TODO
    end
  end

end
