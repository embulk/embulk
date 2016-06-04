base_dir = File.expand_path(File.join(File.dirname(__FILE__), ".."))
lib_dir = File.join(base_dir, "lib")
test_dir = File.join(base_dir, "test")

$LOAD_PATH.unshift(lib_dir)
$LOAD_PATH.unshift(test_dir)

require 'simplecov'
SimpleCov.start 'test_frameworks' do
  add_filter "build/"
end

require "test/unit"
require "test/unit/rr"

Dir.glob("#{base_dir}/test/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,"")
end

exit Test::Unit::AutoRunner.run
