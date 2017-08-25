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

  def self.require_classpath(already_warned=false)
    if __FILE__.include?("!")
      # single jar. __FILE__ should point path/to/embulk.jar!/embulk.rb
      # which means that embulk.jar is already loaded in this JVM.

    elsif __FILE__ =~ /^(?:classpath|uri:classloader):/
      # already in classpath

    else
      # gem package. __FILE__ should point path/to/embulk/lib/embulk.rb
      # that requires here to load ../classpath/*.jar to start EmbulkEmbed.

      unless already_warned
        STDERR.puts "################################################################################"
        STDERR.puts "[WARN] Embulk's gem package is deprecated, and will be removed from v0.9."
        STDERR.puts "[WARN] Use the jar version installed from http://dl.embulk.org/ instead."
        STDERR.puts "[WARN] See the issue and comment at: https://github.com/embulk/embulk/issues/628"
        STDERR.puts "################################################################################"
        STDERR.puts ""
      end

      gem_root = File.expand_path('..', File.dirname(__FILE__))
      classpath_dir = File.join(gem_root, "classpath")
      jars = Dir.entries(classpath_dir).select{|f| f =~ /\.jar$/ }.sort
      jars.each do |jar|
        require File.join(classpath_dir, jar)
      end
    end
  end

  def self.setup(system_config={})
    STDERR.puts "################################################################################"
    STDERR.puts "[WARN] Embulk's gem package is deprecated, and will be removed from v0.9."
    STDERR.puts "[WARN] Use the jar version installed from http://dl.embulk.org/ instead."
    STDERR.puts "[WARN] See the issue and comment at: https://github.com/embulk/embulk/issues/628"
    STDERR.puts "################################################################################"
    STDERR.puts ""

    unless RUBY_PLATFORM =~ /java/i
      raise "Embulk.setup works only with JRuby."
    end

    # require 'json' -- was required to format the system config into a JSON string.

    require_classpath(true)

    require 'embulk/runner'

    # see also embulk/java/bootstrap.rb loaded by JRubyScriptingModule
    runner_java = EmbulkRunner.new(Java::org.embulk.EmbulkSetup::setup(Java::java.util.HashMap.new(system_config)))
    Embulk.const_set :Runner, runner_java
  end
end
