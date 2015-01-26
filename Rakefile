#!/usr/bin/env rake

require 'bundler/gem_tasks'
require 'rake/testtask'
require 'rake/clean'
require 'bundler'
require 'yard'

desc 'Generate YARD document'
YARD::Rake::YardocTask.new(:doc) do |t|
  t.files   = ['lib/**/*.rb']
  t.options = %w[-M kramdown]
  t.options << '--debug' << '--verbose' if $trace
end

desc "Clean java code and copy files"
task :clean do
  sh "mvn clean"
  rm_rf "classpath"
end

desc "Compile java code and copy jar files to classpath/"
task :compile do
  sh "mvn package dependency:copy-dependencies"
  rm_rf "classpath"
  mkdir_p "classpath"
  cp Dir["embulk-cli/target/dependency/*.jar"], "classpath"
  targets = Dir["embulk-cli/target/embulk-cli-*.jar"]
  targets.reject! {|target| target =~ /-sources.jar$/ || target =~ /-executable.jar$/ }
  cp targets, "classpath"
end

desc "Create embulk-{version}.jar"
task :jar do
  require_relative 'lib/embulk/version'
  executable = Dir["embulk-cli/target/embulk-cli-*-executable.jar"].sort.last
  executable_data = File.read(executable).force_encoding('ASCII-8BIT')
  header = <<EOF
#!/bin/sh
exec java -jar "$0" "$@"
exit 127
EOF
  data = header.force_encoding('ASCII-8BIT') + executable_data
  path = "embulk-#{Embulk::VERSION}.jar"
  rm_f path
  File.open(path, 'wb', 0755) {|f| f.write data }
  puts "Created #{path}"
end

desc "Run clean, compile, build and jar"
task :all do
  Rake::Task["clean"].invoke
  Rake::Task["compile"].invoke
  Rake::Task["build"].invoke
  Rake::Task["jar"].invoke
end

task :default => :all
