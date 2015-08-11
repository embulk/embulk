module Embulk
  module Java
    require 'embulk/gems'
    Embulk.add_embedded_gem_path

    require 'embulk/java/imports'
    require 'embulk/java/time_helper'
    require 'embulk/java/liquid_helper'

    module Injected
      # Following constats are set by org.embulk.jruby.JRubyScriptingModule:
      #   Injector
      #   ModelManager
      #   BufferAllocator
    end

    def self.injector
      Injected::Injector
    end
  end
end
