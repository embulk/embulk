
module Embulk
  class Buffer < String
    if Embulk.java?
      def java_object
        Java::BufferBridge.newFromString(self)
      end
    end
  end
end
