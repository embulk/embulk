module Embulk

  class Column < Struct.new(:index, :name, :type, :format)
    def initialize(*args)
      if args.length == 1 && args[0].is_a?(Hash)
        # initialize(hash)
        hash = args.first
        super(hash[:index], hash[:name], hash[:type], hash[:format])
      else
        # initialize(index, name, type, format)
        super(*args)
      end
    end

    def to_json(*args)
      if type == :timestamp && format
        {"index"=>index, "name"=>name, "type"=>type, "format"=>format}.to_json(*args)
      else
        {"index"=>index, "name"=>name, "type"=>type}.to_json(*args)
      end
    end

    def self.from_java(java_column)
      type = Type.from_java(java_column.getType)
      if type == :timestamp
        format = java_column.getType.getFormat
      else
        format = nil
      end

      Column.new(java_column.getIndex, java_column.getName, type, format)
    end

    def to_java
      if type == :timestamp && format
        Java::Column.new(index, name, Type.new_java_type(type).withFormat(format))
      else
        Java::Column.new(index, name, Type.new_java_type(type))
      end
    end
  end

  module Type
    def self.from_java(java_type)
      java_type.getName.to_sym
    end

    def self.new_java_type(ruby_type)
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
      when :json
        Java::Types::JSON
      else
        raise ArgumentError, "Unknown type #{ruby_type.inspect}: supported types are :boolean, :long, :double, :string and :timestamp"
      end
    end
  end

end
