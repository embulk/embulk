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
  sh "./gradlew clean"
end

desc "Build and copy jar files to ./classpath"
task :classpath do
  sh "./gradlew classpath"
end

task :default => :classpath
