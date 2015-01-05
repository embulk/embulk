
module Embulk
  require 'json'
  require 'embulk/data_source'

  module Java
    java_import 'org.embulk.config.DataSourceImpl'
    java_import 'org.embulk.jruby.DataSourceBridge'
  end

  # implements DataSourceBridge::Meta and adds java_object method
  class DataSource
    class <<self
      include Java::DataSourceBridge::Meta

      def convert(json)
        new.merge!(JSON.parse(json))
      end
    end

    def java_object
      json = to_json.to_java
      Java::DataSourceBridge.newFromJson(Java::Injected::ModelManager, json)
    end
  end
end
