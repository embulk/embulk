module Embulk

  require 'embulk/column'
  require 'msgpack'

  class Schema < Array
    def initialize(columns)
      columns = columns.map.with_index {|c,index|
        if c.index && c.index != index
          # TODO ignore this error?
          raise "Index of column '#{c.name}' is #{c.index} but it is at column #{index}."
        end
        Column.new(index, c.name, c.type, c.format)
      }
      super(columns)

      record_reader_script =
        "lambda do |reader|\n" <<
        "record = []\n"
      each do |column|
        idx = column.index
        column_script =
          "value_api = ::Embulk::Java::SPI::Json::RubyValueApi\n" <<
          "if reader.isNull(#{idx})\n" <<
          "record << nil\n" <<
          "else\n" <<
          case column.type
          when :boolean
            "record << reader.getBoolean(#{idx})"
          when :long
            "record << reader.getLong(#{idx})"
          when :double
            "record << reader.getDouble(#{idx})"
          when :string
            "record << reader.getString(#{idx})"
          when :timestamp
            "record << reader.getTimestamp(#{idx}).getRubyTime(JRuby.runtime)"
          when :json
            "record << MessagePack.unpack(value_api.toMessagePack(JRuby.runtime, reader.getJson(#{idx})))"
          else
            raise "Unknown type #{column.type.inspect}"
          end <<
          "end\n"
        record_reader_script << column_script << "\n"
      end
      record_reader_script << "record\n"
      record_reader_script << "end"
      @record_reader = eval(record_reader_script)

      record_writer_script = "lambda do |builder,record|\n"
      record_writer_script << "java_timestamp_class = ::Embulk::Java::Timestamp\n"
      record_writer_script << "value_api = ::Embulk::Java::SPI::Json::RubyValueApi\n"
      each do |column|
        idx = column.index
        column_script =
          "if record[#{idx}].nil?\n" <<
          "builder.setNull(#{idx})\n" <<
          "else\n" <<
          case column.type
          when :boolean
            "builder.setBoolean(#{idx}, record[#{idx}])"
          when :long
            "builder.setLong(#{idx}, record[#{idx}])"
          when :double
            "builder.setDouble(#{idx}, record[#{idx}])"
          when :string
            "builder.setString(#{idx}, record[#{idx}])"
          when :timestamp
            "builder.setTimestamp(#{idx}, java_timestamp_class.fromRubyTime(record[#{idx}]))"
          when :json
            "builder.setJson(#{idx}, value_api.fromMessagePack(MessagePack.pack(record[#{idx}])))"
          else
            raise "Unknown type #{column.type.inspect}"
          end <<
          "end\n"
        record_writer_script << column_script << "\n"
      end
      record_writer_script << "builder.addRecord\n"
      record_writer_script << "end"
      @record_writer = eval(record_writer_script)

      @names = map {|c| c.name }
      @types = map {|c| c.type }

      freeze
    end

    attr_reader :names, :types

    def read_record(page_reader)
      @record_reader.call(page_reader)
    end

    def write_record(page_builder, record)
      @record_writer.call(page_builder, record)
    end

    def self.from_java(java_schema)
      new java_schema.getColumns.map {|column| Column.from_java(column) }
    end

    def to_java
      columns = self.map {|column| column.to_java }
      Java::Schema.new(columns)
    end
  end

end
