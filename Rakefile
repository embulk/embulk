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
  targets.reject! {|target| target =~ /sources/ }
  cp targets, "classpath"
end

desc "Run clean, compile and build"
task :all do
  Rake::Task["clean"].invoke
  Rake::Task["compile"].invoke
  Rake::Task["build"].invoke
end

task :default => :all
