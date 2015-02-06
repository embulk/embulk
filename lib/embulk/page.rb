
module Embulk

  # TODO pure-ruby page reader

  class Page
    include Enumerable

    def initialize(java_page, schema)
      @java_page = java_page
      @schema = schema
    end

    attr_reader :schema

    def each
      schema = @schema
      reader = Java::PageReader.new(schema.to_java)
      begin
        reader.setPage(@java_page)
        while reader.nextRecord
          yield schema.read_record(reader)
        end
      ensure
        reader.close
      end
    end
  end

end
