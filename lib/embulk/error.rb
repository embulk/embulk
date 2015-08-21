
module Embulk
  # ConfigError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues ConfigError.
  class ConfigError < Java::Config::ConfigException
    def initialize(message=nil)
      if message
        super(message.to_s)
      else
        super()
      end
    end
  end

  # DataError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues DataError.
  class DataError < Java::SPI::DataException
    def initialize(message=nil)
      if message
        super(message.to_s)
      else
        super()
      end
    end
  end

  class PluginLoadError < ConfigError
  end
end
