
module Embulk
  module Java
    # Injector is defined by embulk.jruby.JRubyScriptingModule

    module Injected
      def self.inject!(symbol, name)
        const_set(symbol, Injector.getInstance(java.lang.Class.forName(name)))
      end

      inject! :ModelManager, "org.embulk.config.ModelManager"
      inject! :BufferAllocator, "org.embulk.spi.BufferAllocator"
    end
  end
end
