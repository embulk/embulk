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

  module Type
    if Embulk.java?
      def self.from_java_object(java_type)
        java_type.getName.to_sym
      end

      def self.to_java_object(ruby_type)
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
