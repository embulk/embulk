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

require "helper"
require "date"
require "test/unit"

module DateExt
  require "java"

  java_package "org.embulk.spi.time"

  def self.included base
    base.instance_eval do
      def _strptime(str, fmt='%F')
        map = parse_with_embulk_ruby_time_parser(fmt, str)
        return map.nil? ? nil : map.to_hash.inject({}){|hash,(k,v)| hash[k.to_sym] = v; hash}
      end

      def parse_with_embulk_ruby_time_parser(fmt, str)
        format = Java::org.embulk.spi.time.RubyTimeFormat.compile(fmt)
        parser = Java::org.embulk.spi.time.RubyTimeParser.new(format)
        time_parse_result = parser.parse(str)
        if time_parse_result.nil?
          return nil
        end
        return convert_time_parse_result_to_ruby_hash(time_parse_result)
      end

      def convert_time_parse_result_to_ruby_hash(time_parse_result)
        ruby_hash = {}
        time_parse_result.asMapLikeRubyHash().each do |key, value|
          if value.kind_of?(Java::java.math.BigDecimal)
            nanosecond = value.multiply(Java::java.math.BigDecimal::TEN.pow(9)).longValue();
            ruby_hash[key.to_s] = Rational(nanosecond, 10 ** 9)
          else
            ruby_hash[key.to_s] = value
          end
        end
        return ruby_hash
      end

    end
  end
end
Date.send(:include, DateExt)

Dir.glob("#{test_dir}/monkey_strptime/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,"")
end

exit Test::Unit::AutoRunner.run
