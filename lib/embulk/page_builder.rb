module Embulk

  class PageBuilder
    def initialize(schema, java_page_output)
      @page_builder = Java::PageBuilder.new(Java::Injected::BufferAllocator, schema.java_object, java_page_output)
      @schema = schema
    end

    def add(record)
      @schema.write_record(@page_builder, record)
    end

    def finish
      @page_builder.finish
    end

    def close
      @page_builder.close
    end
  end

end
