module Embulk
  module Java
    require 'embulk/java/imports'
    require 'embulk/java/time_helper'

    module Injected
      def self.inject!(symbol, name)
        const_set(symbol, Embulk::Java::Injector.getInstance(java.lang.Class.forName(name)))
      end
    end

    # calledby JRubyScriptingModule
    def self.bootstrap!(injector)
      Embulk::Java.const_set(:Injector, injector)
      Injected.inject! :ModelManager, "org.embulk.config.ModelManager"
      Injected.inject! :BufferAllocator, "org.embulk.spi.BufferAllocator"
    end
  end
end
