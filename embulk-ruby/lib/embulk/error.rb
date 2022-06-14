#
# As of JRuby 9.3, subclassing a Java classes are full java classes,
# which introduces some restrictions:
# See details:
# https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class
# https://github.com/jruby/jruby/issues/7221
#
module Embulk
  # ConfigError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues ConfigError.
  class ConfigError < Java::Config::ConfigException
    def initialize(*arguments)
      super
    end
  end

  # DataError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues DataError.
  class DataError < Java::SPI::DataException
    def initialize(*arguments)
      super
    end
  end

  class PluginLoadError < ConfigError
  end
end
