
module Embulk
  class Buffer < String
    if Embulk.java?
      def self.from_java(java_buffer)
        byte_list = org.jruby.util.ByteList.new(java_buffer.array(), java_buffer.offset(), java_buffer.limit(), false)
        buffer = new
        buffer.replace(org.jruby.RubyString.new(JRuby.runtime, self, byte_list).dup)  # TODO simplify
        buffer
      end

      def to_java
        Java::Buffer.wrap(to_java_bytes)
      end
    end
  end
end
