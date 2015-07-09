
module Embulk
  require 'embulk/buffer'

  class FileInput
    def initialize(java_file_input)
      @java_file_input = java_file_input
      @buffer = nil
    end

    def next_file
      if @java_file_input.nextFile
        return self
      else
        return nil
      end
    end

    def each(&block)
      if @buffer
        yield @buffer
        @buffer = nil
      end

      while java_buffer = @java_file_input.poll
        buffer = Buffer.from_java(java_buffer)
        java_buffer.release
        yield buffer
      end
    end

    def read(count=nil, dest=nil)
      if count == nil
        @buffer ||= Buffer.new
        while java_buffer = @java_file_input.poll
          @buffer << Buffer.from_java(java_buffer)
          java_buffer.release
        end

        return nil if @buffer.empty? && count != 0

        if dest
          dest.replace(@buffer)
        else
          dest = @buffer
        end
        @buffer = nil

      else
        @buffer ||= Buffer.new
        until @buffer.size >= count
          java_buffer = @java_file_input.poll
          break unless java_buffer
          @buffer << Buffer.from_java(java_buffer)
          java_buffer.release
        end

        return nil if @buffer.empty? && count != 0

        if @buffer.size <= count
          if dest
            dest.replace(@buffer)
          else
            dest = @buffer
          end
          @buffer = nil
        else
          data = @buffer.slice!(0, count)
          if dest
            dest.replace(data)
          else
            dest = data
          end
        end
      end
      return dest
    end

    def close
      @java_file_input.close
    end

    def to_java
      @java_file_input
    end
  end
end
