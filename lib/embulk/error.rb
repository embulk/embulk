
module Embulk
  module UserDataError
    include Java::Config::UserDataException
  end

  class ConfigError < StandardError
    include UserDataError
  end

  class DataError < StandardError
    include UserDataError
  end

  class PluginLoadError < StandardError
  end
end
