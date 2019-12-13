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
each_jar_in(File.join(root_dir, 'embulk-deps', 'buffer', 'build', 'dependency_jars')) do |jar|
  static_initializer.addDependency(Java::org.embulk.deps.DependencyCategory::BUFFER, java.nio.file.Paths.get(jar.to_s))
end
each_jar_in(File.join(root_dir, 'embulk-deps', 'guess', 'build', 'dependency_jars')) do |jar|
  static_initializer.addDependency(Java::org.embulk.deps.DependencyCategory::GUESS, java.nio.file.Paths.get(jar.to_s))
end
# https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#calling-masked-or-unreachable-java-methods-with-java_send
static_initializer.java_send :initialize

Gem.path << File.join(core_dir, 'build', 'dependency_gems_as_resources')
Gem.path << File.join(core_dir, 'build', 'embulk_gems_as_resources')
Gem::Specification.reset

=begin
require 'simplecov'
# fix inaccurate coverage
# see: https://github.com/colszowka/simplecov/blob/82920ca1502be78ccde4fd315634066093bb855d/lib/simplecov.rb#L7
ENV['JRUBY_OPTS'] = '-Xcli.debug=true --debug'
SimpleCov.profiles.define 'embulk' do
  add_filter 'test/'

  add_group 'Libraries', 'lib'
end
SimpleCov.start 'embulk'
=end

require 'test/unit'

require 'embulk/java/bootstrap'
require 'embulk'

# "use_global_ruby_runtime" needs to be true because this test process starts from JRuby, the global instance.
embulk_system_properties = Java::java.util.Properties.new()
embulk_system_properties.setProperty("use_global_ruby_runtime", "true")
bootstrap = Java::org.embulk.EmbulkEmbed::Bootstrap.new;
bootstrap.setEmbulkSystemProperties(embulk_system_properties);
Java::org.embulk.EmbulkRunner.new(bootstrap.initialize__method())

Dir.glob("#{this_dir}/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,'')
end

exit Test::Unit::AutoRunner.run
