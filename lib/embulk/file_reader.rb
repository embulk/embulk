module Embulk
  require 'embulk/bridge/buffer'

  class FileReader
    def initialize(fileBufferInput)
      @fileBufferInput = fileBufferInput
    end

    def each(&block)
      @fileBufferInput.each do |buffer|
        yield Bridge::Buffer.to_str(buffer)
      end
    end

    def next_file
      @fileBufferInput.nextFile
    end
  end
end
