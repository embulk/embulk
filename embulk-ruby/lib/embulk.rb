module Embulk
  # logger still should be setup first
  require 'embulk/logger'

  def self.lib_path(path)
    raise NotImplementedError.new("Embulk.lib_path is removed in v0.9.")
  end

  def self.require_classpath()
    raise NotImplementedError.new("Embulk.require_classpath is removed in v0.9.")
  end

  def self.setup(system_config={})
    raise NotImplementedError.new("Embulk.setup is removed in v0.9.")
  end
end
