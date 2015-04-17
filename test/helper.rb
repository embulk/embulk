require 'test/unit'

classpath_dir = File.expand_path('../classpath', File.dirname(__FILE__))
jars = Dir.entries(classpath_dir).select {|f| f =~ /\.jar$/ }.sort
jars.each do |jar|
  require File.join(classpath_dir, jar)
end

require 'embulk/java/bootstrap'
require 'embulk'

# TODO simplecov
