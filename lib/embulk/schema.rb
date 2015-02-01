module Embulk

  require 'embulk/column'

  class Schema < Array
    def initialize(src)
      super

      record_reader_script =
        "lambda do |reader|\n" <<
        "record = []\n"
      each do |column|
        idx = column.index
        column_script =
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

    if Embulk.java?
      def self.from_java_object(java_schema)
        new java_schema.getColumns.map {|column| Column.from_java_object(column) }
      end

      def java_object
        columns = self.map {|column| column.java_object }
        Java::Schema.new(columns)
      end
    end
  end

end
