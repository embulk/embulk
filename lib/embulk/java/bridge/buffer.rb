
module Embulk
  require 'embulk/buffer'

  module Java
    java_import 'org.embulk.spi.Buffer'
    java_import 'org.embulk.jruby.BufferBridge'
  end

  # adds java_object method
  class Buffer
    def java_object
      Java::BufferBridge.newFromString(str)
    end
  end
end
