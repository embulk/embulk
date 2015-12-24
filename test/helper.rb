require 'test/unit'

classpath_dir = File.expand_path('../classpath', File.dirname(__FILE__))
jars = Dir.entries(classpath_dir).select {|f| f =~ /\.jar$/ }.sort
jars.each do |jar|
  require File.join(classpath_dir, jar)
end

require 'simplecov'
# fix inaccurate coverage
# see: https://github.com/colszowka/simplecov/blob/82920ca1502be78ccde4fd315634066093bb855d/lib/simplecov.rb#L7
ENV['JRUBY_OPTS'] = '-Xcli.debug=true --debug'
SimpleCov.profiles.define 'embulk' do
  add_filter 'test/'

  add_group 'Libraries', 'lib'
end
SimpleCov.start 'embulk'

require 'embulk/java/bootstrap'
require 'embulk'

Embulk.setup
