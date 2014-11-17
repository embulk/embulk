module QuickLoad
  module Bridge
    class Schema < Struct.new(:columns)
      def initialize(columns)
        super(columns)
      end

      def self.wrap(schema)
        new(schema.getColumns.to_a)
      end

      def to_java
        java_columns = columns.map {|c| c.to_java }
        Java::Schema.new(java_columns)
      end
    end

    class Column < Struct.new(:index, :name, :type)
      def initialize(index, name, type)
        super(index, name, type.getName.to_sym)
        @java_type = type
      end

      attr_reader :java_type

      def to_java
        Java::Column.new(index, name, @java_type)
      end
    end
  end
end
