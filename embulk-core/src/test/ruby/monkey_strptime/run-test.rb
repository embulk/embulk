# Tests org.embulk.spi.time.RubyDateParser, Embulk's own time parser compatible with Ruby's Date._strptime.
# It runs tests imported from MRI (Matz' Ruby Implementation) against org.embulk.spi.time.RubyDateParser.

this_dir = File.dirname(__FILE__)
core_dir = File.expand_path(File.join(this_dir, '..', '..', '..', '..'))

$CLASSPATH << File.join(core_dir, 'build', 'classes', 'java', 'main')

require 'date'
require 'test/unit'

module DateExt
  require 'java'

  java_package 'org.embulk.spi.time'

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

Dir.glob("#{this_dir}/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,'')
end

exit Test::Unit::AutoRunner.run
