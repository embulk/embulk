require 'java'

java_package 'org.quickload.jruby'
java_import 'com.google.inject.Injector'
java_import 'com.google.inject.Inject'
java_import 'org.quickload.plugin.PluginSourceNotMatchException'
java_import 'com.fasterxml.jackson.databind.JsonNode'
java_import 'org.quickload.plugin.PluginSource'

class JRubyPluginSource
  java_implements 'PluginSource'

  java_annotation 'Inject'
  java_signature 'JRubyPluginSource(Injector injector)'
  def initialize(injector)
    @injector = injector
  end

  java_signature 'Object newPlugin(Class iface, JsonNode typeConfig) throws PluginSourceNotMatchException'
  def newPlugin(iface, typeConfig)
    # TODO
    raise PluginSourceNotMatchException.new
  end
end
