module Embulk
  # logger should be setup first
  require 'embulk/logger'

  def self.lib_path(path)
    STDERR.puts "################################################################################"
    STDERR.puts "[WARN] Embulk.lib_path is deprecated. It will be removed in later versions."
    STDERR.puts "################################################################################"

    path = '' if path == '/'
    jar, resource = __FILE__.split("!", 2)
    if resource
      lib = resource.split("/")[0..-2].join("/")
      "#{jar}!#{lib}/#{path}"
    elsif __FILE__ =~ /^(?:classpath|uri:classloader):/
      lib = __FILE__.split("/")[0..-2].join("/")
      "#{lib}/#{path}"
    else
      lib = File.expand_path File.dirname(__FILE__)
      File.join(lib, *path.split("/"))
    end
  end

  def self.require_classpath()
    raise NotImplementedError.new("Embulk.require_classpath is removed in v0.9.")
  end

  def self.setup(system_config={})
    # TODO: Remove Embulk.setup, and describe how to test Ruby plugins.
    STDERR.puts "################################################################################"
    STDERR.puts "[WARN] Embulk.setup is deprecated. It will be removed in later versions."
    STDERR.puts "################################################################################"

    require 'embulk/runner'

    # see also embulk/java/bootstrap.rb loaded by JRubyScriptingModule
    runner_java = EmbulkRunner.new(Java::org.embulk.EmbulkSetup::setup(Java::java.util.HashMap.new(system_config)))
    Embulk.const_set :Runner, runner_java
  end
end
