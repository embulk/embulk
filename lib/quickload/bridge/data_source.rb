
module QuickLoad
  module Bridge
    require 'json'

    class DataSourceBridge < Hash
      def initialize(java_class)
        super()
        @java_class = java_class
      end

      def self.wrap(data_source)
        objectNode = JSON.parse(data_source.toString)  # TODO optimize
        return new(data_source.class).merge!(objectNode)
      end

      def to_java
        @java_class.new(DataSource.parseJson(self.to_json))
      end

      def self.to_java_config_source(hash)
        ConfigSource.new.setAll(DataSource.new(DataSource.parseJson(hash.to_json)))
      end

      def self.to_java_task_source(hash)
        TaskSource.new.setAll(DataSource.new(DataSource.parseJson(hash.to_json)))
      end

      def self.to_java_next_config(hash)
        NextConfig.new.setAll(DataSource.new(DataSource.parseJson(hash.to_json)))
      end

      def self.to_java_report(hash)
        Report.new.stAll(DataSource.new(DataSource.parseJson(hash.to_json)))
      end
    end
  end
end
