
module QuickLoad::Bridge
  class DataSourceBridge
    def initialize(data_source)
      @data_source = data_source
    end

    def [](key)
      # TODO
    end

    def []=(key, value)
      # TODO
    end

    def load(key, type, options={})
      # TODO
    end

    def store(key, type=nil)
      # TODO
    end

    def keys
      @data_source.keys
    end

    def self.wrap(data_source)
      DataSourceBridge.new(data_source)
    end
  end
end
