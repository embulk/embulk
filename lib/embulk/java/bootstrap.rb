module Embulk
  module Java
    require 'embulk/java/imports'
    require 'embulk/java/time_helper'

    module Injected
      # Following constats are set by org.embulk.jruby.JRubyScriptingModule:
      #   Injector
      #   ModelManager
      #   BufferAllocator
    end

    def self.injector
      Injected::Injector
    end

    require 'embulk'
    require 'embulk/error'
    require 'embulk/buffer'
    require 'embulk/data_source'
    require 'embulk/plugin'
    require 'embulk/runner'
  end
end
