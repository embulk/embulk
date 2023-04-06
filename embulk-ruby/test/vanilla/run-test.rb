# Tests guess, and org.embulk.spi.time.TimestampParser which parses timestamp strings into Embulk's Timestamp.

deps_classpath = Java::java.lang.System.getProperty("deps_classpath")
if deps_classpath.nil?
  raise Java::java.lang.NullPointerException.new("System property \"deps_classpath\" is not set.")
end

static_initializer = Java::org.embulk.EmbulkDependencyClassLoader.staticInitializer()
deps_classpath.split(Regexp.escape(Java::java.io.File::pathSeparator)).each do |path|
  static_initializer.addDependency(java.nio.file.Paths.get(path))
end
# https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#calling-masked-or-unreachable-java-methods-with-java_send
static_initializer.java_send :initialize

require 'test/unit'

require 'embulk/java/bootstrap'
require 'embulk'

# "use_global_ruby_runtime" needs to be true because this test process starts from JRuby, the global instance.
properties = Java::java.util.Properties.new()
properties.setProperty("use_global_ruby_runtime", "true")
embulk_system_properties = Java::org.embulk.EmbulkSystemProperties.of(properties)
bootstrap = Java::org.embulk.EmbulkEmbed::Bootstrap.new;
bootstrap.setEmbulkSystemProperties(embulk_system_properties);
Java::org.embulk.EmbulkRunner.new(bootstrap.initialize__method(), embulk_system_properties)

this_dir = File.dirname(__FILE__)
Dir.glob("#{this_dir}/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,'')
end

exit Test::Unit::AutoRunner.run
