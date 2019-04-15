module Embulk
  module Java
    require 'embulk/java/imports'
    require 'time'

    module Injected
      def Injected.const_missing(id)
        case id
        when :Injector then
          raise NotImplementedError, "The constant Embulk::Java::Injected::Injector is no longer available."
        when :ModelManager then
          raise NotImplementedError, "The constant Embulk::Java::Injected::ModelManager is no longer available."
        when :BufferAllocator then
          raise NotImplementedError, "The constant Embulk::Java::Injected::BufferAllocator is no longer available."
        end
      end
    end

    def self.injector
      raise NotImplementedError, "The method Embulk::Java.injector is no longer available."
    end

    require 'embulk'
    require 'embulk/error'
    require 'embulk/buffer'
    require 'embulk/data_source'
    require 'embulk/plugin'
  end
end
