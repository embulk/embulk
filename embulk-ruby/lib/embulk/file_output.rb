
module Embulk
  require 'embulk/buffer'

  class FileOutput
    def initialize(java_file_output)
      @java_file_output = java_file_output
      @buffer = Buffer.new
      @buffer.force_encoding('ASCII-8BIT')
      @flush_size = 32*1024
    end

    def next_file
      flush
      @java_file_output.nextFile
      self
    end

    def write(buffer)
      buffer.force_encoding('ASCII-8BIT')  # TODO this is destructively change buffer
      @buffer << buffer
      if @buffer.size > @flush_size
        flush
      end
      nil
    end

    def add(buffer)
      flush
      @java_file_output.add(Buffer.from_ruby_string(buffer))
      nil
    end

    def flush
      unless @buffer.empty?
        @java_file_output.add(@buffer.to_java)
        @buffer.clear
      end
      nil
    end

    def finish
      flush
      @java_file_output.finish
    end

    def close
      @java_file_output.close
    end

    def to_java
      @java_file_output
    end
  end

end
