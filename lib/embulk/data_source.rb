module Embulk
  require 'json'

  class DataSource < Hash
    def prop(key, type, options={})
      if self.has_key?(key)
        v = self[key]
        value =
          case type
          when :int
            Integer(v)
          when :float
            Float(v)
          when :string
            String(v)
          when :bool
            !!v  # TODO validation
          when :hash
            raise ArgumentError, "Invalid value for :hash" unless v.is_a?(Hash)
            v
          when :array
            raise ArgumentError, "Invalid value for :array" unless v.is_a?(Array)
            v
          else
            unless type.respond_to?(:load)
              raise ArgumentError, "Unknown type #{type.to_s.dump}"
            end
            type.load(v)
          end

      elsif options.has_key?(:default)
        value = options[:default]

      else
        raise "Required field #{key.to_s.dump} is not set"
      end

      return value
    end

    if Embulk.java?
      def self.from_java_object(java_data_source_impl)
        json = java_data_source_impl.toString
        new.merge!(JSON.parse(json))
      end

      def java_object
        json = to_json
        Java::Injected::ModelManager.readObject(Java::DataSourceImpl.java_class, json.to_java)
      end

      def load_config(task_type)
        Java::Injected::ModelManager.readObjectWithConfigSerDe(task_type.java_class, to_json.to_java)
      end

      def load_task(task_type)
        Java::Injected::ModelManager.readObject(task_type.java_class, to_json.to_java)
      end
    end
  end

end
