module Embulk

  class Column < Struct.new(:index, :name, :type, :options)
    def initialize(*args)
      if args.length == 1 && args[0].is_a?(Hash)
        # initialize(hash)
        options = args.first.dup
        super(options.delete(:index), options.delete(:name), options.delete(:type), DataSource.new(options))
      else
        # initialize(index, name, type, options)
        super(args[0], args[1], args[2], DataSource.new(args[3] || {}))
      end
    end

    # obsoleted
    def format
      options[:format]
    end

    def to_json(*args)
      if type == :timestamp && format
        {"index"=>index, "name"=>name, "type"=>type, "format"=>format}.to_json(*args)
      else
        {"index"=>index, "name"=>name, "type"=>type}.to_json(*args)
      end
    end

    if Embulk.java?
      def self.from_java(java_column)
        type = Type.from_java(java_column.getType)
        options = {}
        if type == :timestamp
          options[:format] = java_column.getType.getFormat
        end

        Column.new(java_column.getIndex, java_column.getName, type, options)
      end

      def to_java
        if type == :timestamp && format
          t = Type.new_java_type(type)
        else
          t = Type.new_java_type(type)
        end
        Java::Column.new(index, name, t.to_java, options.to_java)
      end
    end
  end

  module Type
    if Embulk.java?
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
        else
          raise ArgumentError, "Unknown type #{ruby_type.inspect}: supported types are :boolean, :long, :double, :string and :timestamp"
        end
      end
    end
  end

end
