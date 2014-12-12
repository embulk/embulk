
module Embulk
  module Bridge
    require 'json'
    require 'embulk/config_hash'

    class DataSourceBridge < ConfigHash
      def initialize(java_class)
        super()
        @java_class = java_class
      end

      def self.wrap(data_source)
        objectNode = JSON.parse(data_source.toString)  # TODO optimize
        return new(data_source.class).merge!(objectNode)
      end

      def to_java
        @java_class.new(Java::DataSource.parseJson(self.to_json))
      end

      def self.to_java_config_source(hash)
        Java::ConfigSource.new.setAll(to_java_object_node(hash))
      end

      def self.to_java_task_source(hash)
        Java::TaskSource.new.setAll(to_java_object_node(hash))
      end

      def self.to_java_next_config(hash)
        Java::NextConfig.new.setAll(to_java_object_node(hash))
      end

      def self.to_java_report(hash)
        Java::Report.new.setAll(to_java_object_node(hash))
      end

      def self.to_java_object_node(hash)
        Java::DataSource.parseJson(hash.to_json)
      end
    end
  end
end
