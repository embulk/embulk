module Embulk
  # logger should be setup first
  require 'embulk/logger'

  def self.lib_path(path)
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

  def self.require_classpath
    if __FILE__.include?("!")
      # single jar. __FILE__ should point path/to/embulk.jar!/embulk.rb
      # which means that embulk.jar is already loaded in this JVM.

    elsif __FILE__ =~ /^(?:classpath|uri:classloader):/
      # already in classpath

    else
      # gem package. __FILE__ should point path/to/embulk/lib/embulk.rb
      # that requires here to load ../classpath/*.jar to start EmbulkEmbed.
      gem_root = File.expand_path('..', File.dirname(__FILE__))
      classpath_dir = File.join(gem_root, "classpath")
      jars = Dir.entries(classpath_dir).select{|f| f =~ /\.jar$/ }.sort
      jars.each do |jar|
        require File.join(classpath_dir, jar)
      end
    end
  end

  def self.setup(system_config={})
    unless RUBY_PLATFORM =~ /java/i
      raise "Embulk.setup works only with JRuby."
    end

    require 'json'

    require_classpath

    systemConfigJson = system_config.merge({
      # use the global ruby runtime for all ScriptingContainer
      # injected by org.embulk.jruby.JRubyScriptingModule
      use_global_ruby_runtime: true
    }).to_json

    bootstrap = org.embulk.EmbulkEmbed::Bootstrap.new
    systemConfig = bootstrap.getSystemConfigLoader.fromJsonString(systemConfigJson)
    bootstrap.setSystemConfig(systemConfig)
    embed = bootstrap.java_method(:initialize).call  # see embulk-core/src/main/java/org/embulk/jruby/JRubyScriptingModule.

    # see also embulk/java/bootstrap.rb loaded by JRubyScriptingModule

    Embulk.const_set :Runner, EmbulkRunner.new(embed)
  end
end
