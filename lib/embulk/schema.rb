module Embulk

  require 'embulk/column'

  class Schema < Array
    def initialize(src)
      super

      record_reader_script = "lambda do |reader|\n"
      record_reader_script << "record = []\n"
      each do |column|
        column_script =
          case column.type
          when :boolean
            "record << reader.getBoolean(#{column.index})"
          when :long
            "record << reader.getLong(#{column.index})"
          when :double
            "record << reader.getDouble(#{column.index})"
          when :string
            "record << reader.getString(#{column.index})"
          when :timestamp
            "record << reader.getTimestamp(#{column.index})"  # TODO convert to Time
          else
            raise "Unknown type #{column.type.inspect}"
          end
        record_reader_script << column_script << "\n"
      end
      record_reader_script << "record\n"
      record_reader_script << "end"
      @record_reader = eval(record_reader_script)

      record_writer_script = "lambda do |builder,record|\n"
      record_writer_script << ""
      each do |column|
        column_script =
          case column.type
          when :boolean
            "builder.setBoolean(#{column.index}, record[#{column.index}])"
          when :long
            "builder.setLong(#{column.index}, record[#{column.index}])"
          when :double
            "builder.setDouble(#{column.index}, record[#{column.index}])"
          when :string
            "builder.setString(#{column.index}, record[#{column.index}])"
          when :timestamp
            "builder.setTimestamp(#{column.index}, record[#{column.index}])"  # TODO convert to Time
          else
            raise "Unknown type #{column.type.inspect}"
          end
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
