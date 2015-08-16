module Embulk
  if RUBY_PLATFORM =~ /java/i
    def self.java?
      true
    end
  else
    def self.java?
      false
    end
  end

  # logger should be setup first
  require 'embulk/logger'

  def self.lib_path(path)
    path = '' if path == '/'
    jar, resource = __FILE__.split("!", 2)
    if resource
      lib = resource.split("/")[0..-2].join("/")
      "#{jar}!#{lib}/#{path}"
    elsif __FILE__ =~ /^classpath:/
      lib = __FILE__.split("/")[0..-2].join("/")
      "#{lib}/#{path}"
    else
      lib = File.expand_path File.dirname(__FILE__)
      File.join(lib, *path.split("/"))
    end
  end

  def self.require_classpath
    if __FILE__.include?("!")
      # single jar
      jar, resource = __FILE__.split("!", 2)
      require jar

    elsif __FILE__ =~ /^classpath:/
      # already in classpath

    else
      # gem package
      gem_root = File.expand_path('..', File.dirname(__FILE__))
      classpath_dir = File.join(gem_root, "classpath")
      jars = Dir.entries(classpath_dir).select{|f| f =~ /\.jar$/ }.sort
      jars.each do |jar|
        require File.join(classpath_dir, jar)
      end
    end
  end

  def self.setup(system_config={})
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

    require 'embulk/error'
    require 'embulk/buffer'
    require 'embulk/data_source'
    require 'embulk/plugin'
    require 'embulk/runner'

    Embulk.const_set :Runner, EmbulkRunner.new(embed)
  end
end
