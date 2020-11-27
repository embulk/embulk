# Tests guess of embulk-standards.

def each_jar_in(path)
  Dir.entries(path).select{|f| f =~ /\.jar$/}.map{|f| File.join(path, f)}.each do |jar|
    yield jar
  end
end

this_dir = File.dirname(__FILE__)
core_dir = File.expand_path(File.join(this_dir, '..', '..', '..', '..'))
root_dir = File.expand_path(File.join(core_dir, '..'))

embulk_jar_dir = File.join(core_dir, 'build', 'libs')
dependency_jars_dir = File.join(core_dir, 'build', 'dependency_jars')

embulk_jars = Dir.entries(embulk_jar_dir).select{|f| f =~ /\.jar$/}.map{|f| File.join(embulk_jar_dir, f)}
embulk_jars.each do |embulk_jar|
  $CLASSPATH << embulk_jar
end
dependency_jars = Dir.entries(dependency_jars_dir).select{|f| f =~ /\.jar$/}.map{|f| File.join(dependency_jars_dir, f)}
dependency_jars.each do |dependency_jar|
  $CLASSPATH << dependency_jar
end

static_initializer = Java::org.embulk.deps.EmbulkDependencyClassLoaders.staticInitializer()
each_jar_in(File.join(root_dir, 'embulk-deps', 'build', 'dependency_jars')) do |jar|
  static_initializer.addDependency(java.nio.file.Paths.get(jar.to_s))
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

Dir.glob("#{this_dir}/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,'')
end

exit Test::Unit::AutoRunner.run
