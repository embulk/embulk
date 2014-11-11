
module QuickLoad::Bridge
  class BufferBridge
    def self.to_str(buffer)
      bytes = buffer.get
      limit = buffer.limit
      String.from_java_bytes(bytes)[0, limit]
    end
  end
end
