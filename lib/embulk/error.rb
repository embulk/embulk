
module Embulk
  # ConfigError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues ConfigError.
  class ConfigError < Java::Config::ConfigException
  end

  # DataError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues DataError.
  class DataError < Java::SPI::DataException
  end

  class PluginLoadError < ConfigError
  end
end
