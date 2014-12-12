module Embulk
  require 'embulk/bridge/buffer'

  class FileWriter
    def initialize(task_config, fileBufferOutput)
      @fileBufferOutput = fileBufferOutput
    end

    def write(data)
      buffer = Bridge::BufferBridge.to_java_buffer(data)
      @fileBufferOutput.add(buffer)
    end

    alias << write

    def add_file
      @fileBufferOutput.addFile
    end

    def added_size
      @fileBufferOutput.getAddedSize
    end
  end

end
