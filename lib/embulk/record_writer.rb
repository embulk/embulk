module Embulk
  require 'embulk/java/imports'

  class RecordWriter
    def initialize(task_config, pageOutput)
      exec = task_config.java_exec
      @schema = Bridge::Schema.wrap(exec.getSchema)
      @pageBuilder = pageBuilder = Java::PageBuilder.new(exec.getBufferAllocator, exec.getSchema, pageOutput)
      @setters = @schema.columns.map do |column|
        type = column.type
        index = column.index
        case type
        when :string
          lambda {|value| type.setString(pageBuilder, index, String(value)) }
        when :long
          lambda {|value| type.setLong(pageBuilder, index, Integer(value)) }
        when :double
          lambda {|value| type.setLong(pageBuilder, index, Float(value)) }
        when :boolean
          lambda {|value| type.setLong(pageBuilder, index, Boolean(value)) }
        #when :timestamp
        #  lambda {|value| type.setLong(pageBuilder, index, Time.parse(value.to_s)) }
        else
          raise ArgumentError, "Unknown type #{type.inspect}"
        end
      end
    end

    attr_reader :schema

    def columns
      @schema.columns
    end

    def set_column(index, value)
      @setters[index].call(value)
    end

    def add_record(&block)
      if block_given?
        @schema.columns.each do |column|
          set_column(column.index, block.call(column.type, column.index))
        end
      end
      @pageBuilder.addRecord
    end

    def flush
      @pageBuilder.flush
    end
  end
end
