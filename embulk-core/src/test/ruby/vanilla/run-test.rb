root_dir = File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "..", "..", ".."))
src_dir = File.join(root_dir, "embulk-core", "src")
lib_dir = File.join(src_dir, "main", "ruby")
test_dir = File.join(src_dir, "test", "ruby")

$LOAD_PATH.unshift(lib_dir)
$LOAD_PATH.unshift(test_dir)

if ARGV.empty?
  STDERR.puts "No JAR is specified in ARGV. Looking for Embulk core's JAR in: #{root_dir}/classpath"
  found_jar_files = Dir.glob("#{root_dir}/classpath/embulk-core-*.jar")
  if found_jar_files.empty?
    raise IOError, "Embulk core's JAR not found."
  end
  if found_jar_files.length > 1
    raise IOError, "Mulitiple files found like Embulk core's JAR."
  end
  embulk_core_jar_path = found_jar_files[0]
  STDERR.puts "Found. Loading: #{embulk_core_jar_path}"
else
  embulk_core_jar_path = ARGV[0]
end
# This `require` makes gems embedded in embulk-core-*.jar available to be required.
require "#{embulk_core_jar_path}"

# This Gem.path trick is needed to load gems embedded in embulk-core.jar when run through jruby-gradle-plugin.
# It can be after `require "embulk-core.jar"`.
Gem.path << 'uri:classloader://'
Gem::Specification.reset

require "test/unit"

Dir.glob("#{test_dir}/vanilla/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,"")
end

exit Test::Unit::AutoRunner.run
