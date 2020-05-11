module Embulk
  class Buffer < String
    def self.from_java(java_buffer)
      byte_list = org.jruby.util.ByteList.new(java_buffer.array(), java_buffer.offset(), java_buffer.limit(), false)
      buffer = new
      buffer.replace(org.jruby.RubyString.new(JRuby.runtime, self, byte_list).dup)  # TODO simplify
      buffer
    end

    def self.from_ruby_string(string)
      b = Buffer.new(string)
      b.force_encoding('ASCII-8BIT')
    end

    def to_java
      Java::org.embulk.spi.BufferImpl.wrap(to_java_bytes)
    end
  end
end
