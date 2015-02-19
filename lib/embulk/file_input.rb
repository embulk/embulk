
module Embulk
  require 'embulk/buffer'

  class FileInput
    def initialize(java_file_input)
      @java_file_input = java_file_input
    end

    def next_file
      if @java_file_input.nextFile
        return self
      else
        return nil
      end
    end

    def each(&block)
      while java_buffer = @java_file_input.poll
        buffer = Buffer.from_java(java_buffer)
        java_buffer.release
        yield buffer
      end
    end

    def close
      @java_file_input.close
    end
  end
end
