module Embulk
  module Java
    require 'embulk/java/imports'
    require 'time'

    module Injected
      # Following constats are set by org.embulk.jruby.JRubyInitializer:
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
  end
end
