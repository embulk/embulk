
module QuickLoad
  module Bridge
    class BufferBridge
      def self.to_str(buffer)
        bytes = buffer.get
        limit = buffer.limit
        String.from_java_bytes(bytes)[0, limit]
      end

      def self.to_java_buffer(str)
        Java::Buffer.wrap(str.to_java_bytes)
      end
    end
  end
end
