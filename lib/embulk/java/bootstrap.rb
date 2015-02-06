module Embulk
  module Java
    require 'embulk/java/imports'
    require 'embulk/java/time_helper'

    module Injected
      # Following constats are set by org.embulk.jruby.JRubyScriptingModule:
      #   ModelManager
      #   BufferAllocator
    end

    def self.injector
      # TODO use org.embulk.spi.Exec.getInjector
      Injector
    end
  end
end
