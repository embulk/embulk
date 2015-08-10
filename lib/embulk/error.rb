
module Embulk
  if java?
    config_exception = org.embulk.config.ConfigException
  else
    config_exception = StandardError
  end

  class ConfigError < config_exception
  end

  class PluginLoadError < StandardError
  end
end
