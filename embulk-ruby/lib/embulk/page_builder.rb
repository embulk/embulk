module Embulk

  org.embulk.spi.util.dynamic.AbstractDynamicColumnSetter.module_eval do
    def set(ruby_object)
      # Using |java_send| so that it calls Java's correct overloaded |set| method, not Ruby's itself.
      if ruby_object.nil?
        self.java_send(:setNull, [])
      elsif ruby_object == true || ruby_object == false
        self.java_send(:set, [::Java::boolean], ruby_object.to_java(:boolean))
      elsif ruby_object.kind_of?(Integer)
        self.java_send(:set, [::Java::long], ruby_object.to_java(:long))
      elsif ruby_object.kind_of?(Float)
        self.java_send(:set, [::Java::double], ruby_object.to_java(:double))
      elsif ruby_object.kind_of?(String)
        self.java_send(:set, [::Java::java.lang.String], ruby_object.to_java(::Java::java.lang.String))
      elsif ruby_object.kind_of?(Time)
        self.java_send(:set, [::Java::java.time.Instant],
                       ::Java::java.time.Instant.ofEpochSecond(ruby_object.to_i, ruby_object.nsec))
      else
        self.java_send(:set, [::Java::org.msgpack.value.Value],
                       ::Java::org.msgpack.core.MessagePack.newDefaultUnpacker(ruby_object.to_msgpack.to_java_bytes).unpackValue())
      end
    end
  end

  class PageBuilder
    def initialize(schema, java_page_output=nil, java_dynamic_page_builder: nil)
      if java_page_output.nil? && java_dynamic_page_builder.nil?
        raise ArgumentError, 'PageBuilder.new must take either of java_page_output or java_dynamic_page_builder'
      end
      if java_page_output && java_dynamic_page_builder
        raise ArgumentError, 'PageBuilder.new must not take both java_page_output and java_dynamic_page_builder'
      end

      require 'msgpack'  # used at DynamicPageBuilder.set(Value)

      if java_page_output
        # TODO get task as an argument
        task = Java::SPI::Exec.newConfigSource.load_config(Java::DynamicPageBuilder::BuilderTask.java_class)
        @page_builder = Java::DynamicPageBuilder.createWithTimestampMetadataFromBuilderTask(
            task, Java::BufferAllocator, schema.to_java, java_page_output)
      else
        @page_builder = java_dynamic_page_builder
      end

      @schema = schema
    end

    def add(record)
      i = 0
      m = record.size
      while i < m
        @page_builder.column(i).set(record[i])
        i += 1
      end
      @page_builder.addRecord
      nil
    end

    def [](index_or_column)
      case index_or_column
      when Integer
        @page_builder.column_or_null(index_or_column)
      when Column
        @page_builder.column_or_null(index_or_column.index)
      else
        @page_builder.column_or_null(index_or_column)
      end
    end

    def column(index_or_column)
      case index_or_column
      when Integer
        @page_builder.column(index_or_column)
      when Column
        @page_builder.column(index_or_column.index)
      else
        @page_builder.lookupColumn(index_or_column)
      end
    end

    def column_or_skip(index_or_column)
      case index_or_column
      when Integer
        @page_builder.column_or_skip(index_or_column)
      when Column
        @page_builder.column_or_skip(index_or_column.index)
      else
        @page_builder.column_or_skip(index_or_column)
      end
    end

    def add!
      @page_builder.add_record
    end

    def flush
      @page_builder.flush
    end

    def finish
      @page_builder.finish
    end

    def close
      @page_builder.close
    end
  end

end
