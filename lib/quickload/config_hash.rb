module QuickLoad

  class ConfigHash < Hash
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
  end

end
