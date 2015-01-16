module Embulk

  class Column < Struct.new(:index, :name, :type)
    def initialize(index, name, type)
      super(index, name, type)
    end

    if Embulk.java?
      def self.from_java_object(java_column)
        Column.new(
          java_column.getIndex,
          java_column.getName,
          Type.from_java_object(java_column.getType))
      end

      def java_object
        Java::Column.new(index, name, Type.to_java_object(type))
      end
    end
  end

  class Schema < Array
    def initialize(src)
      super
      freeze
      page_reader_script = "lambda do |reader|\n"
      page_reader_script << "record = []\n"
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
          end
        page_reader_script << column_script << "\n"
      end
      page_reader_script << "end"
      @page_reader = eval(page_reader_script)
    end

    def read_record(page_reader)
      @page_reader.call(page_reader)
    end

    if Embulk.java?
      def self.from_java_object(java_schema)
        java_schema.getColumns
      end

      def java_object
        columns = self.map {|column| Column.java_object }
        Java::Schema.new(columns)
      end
    end
  end

  module Type
    if Embulk.java?
      def self.from_java_object(java_type)
        java_type.getType.to_sym
      end

      def to_java_object(ruby_type)
        case ruby_type
        when :boolean
          Java::Types::BOOLEAN
        when :long
          Java::Types::LONG
        when :double
          Java::Types::DOUBLE
        when :string
          Java::Types::STRING
        when :timestamp
          Java::Types::TIMESTAMP
        else
          raise ArgumentError, "Unknown type #{ruby_type.inspect}: supported types are :boolean, :long, :double, :string and :timestamp"
        end
      end
    end
  end

end
