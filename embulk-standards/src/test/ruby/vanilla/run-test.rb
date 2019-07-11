# Tests guess of embulk-standards.

this_dir = File.dirname(__FILE__)
core_dir = File.expand_path(File.join(this_dir, '..', '..', '..', '..'))
root_dir = File.expand_path(File.join(this_dir, '..', '..', '..', '..', '..', 'embulk-core'))
embulk_jar_dir = File.join(core_dir, 'build', 'libs')
embulk_test_path = File.join(root_dir, 'build', 'classes', 'java', 'test')
dependency_jars_dir = File.join(core_dir, 'build', 'dependency_jars')
test_dependency_jars_dir = File.join(core_dir, 'build', 'test_dependency_jars')

$CLASSPATH << embulk_test_path
embulk_jars = Dir.entries(embulk_jar_dir).select{|f| f =~ /\.jar$/}.map{|f| File.join(embulk_jar_dir, f)}
embulk_jars.each do |embulk_jar|
  $CLASSPATH << embulk_jar
end
dependency_jars = Dir.entries(dependency_jars_dir).select{|f| f =~ /\.jar$/}.map{|f| File.join(dependency_jars_dir, f)}
dependency_jars.each do |dependency_jar|
  $CLASSPATH << dependency_jar
end
test_dependency_jars = Dir.entries(test_dependency_jars_dir).select{|f| f =~ /\.jar$/}.map{|f| File.join(test_dependency_jars_dir, f)}
test_dependency_jars.each do |test_dependency_jar|
  $CLASSPATH << test_dependency_jar
end

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
Java::org.embulk.EmbulkSetup::setup(Java::java.util.HashMap.new({"use_global_ruby_runtime": true}))

Dir.glob("#{this_dir}/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,'')
end

modules = Java::java.util.ArrayList.new
modules.add(Java::org.embulk.EmbulkTestRuntime::TestRuntimeModule.new)

injector = Java::com.google.inject.Guice.createInjector(Java::java.util.Collections.unmodifiableList(modules))

execConfig = Java::org.embulk.config.DataSourceImpl.new(injector.getInstance(Java::org.embulk.config.ModelManager.java_class))
execSession = Java::org.embulk.spi.ExecSession.builder(injector).fromExecConfig(execConfig).build()

execAction = Class.new(Java::org.embulk.spi.ExecAction.class) {
  def initialize
    @exit_value = -1
  end
  def run
    @exit_value = Test::Unit::AutoRunner.run
  end
  def exit_value
    @exit_value
  end
}.new()

Java::org.embulk.spi.Exec.doWith(execSession, execAction)

exit execAction.exit_value
