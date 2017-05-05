# How to use this script
# $ jruby embulk-jruby-strptime/test/run-test.rb
#
require "test/unit"

base_dir = File.expand_path(File.join(File.dirname(__FILE__), ".."))
test_dir = File.join(base_dir, "test")

require "#{base_dir}/build/libs/embulk-jruby-strptime-0.8.18.jar"
require 'date'

module DateExt
  def self.included base
    base.instance_eval do
      def _strptime(str, fmt='%F')
        parser = org.embulk.spi.time.RubyDateParser.new(JRuby.runtime.current_context)
        map = parser.parse(fmt, str)
        return map.nil? ? nil : map.to_hash.inject({}){|hash,(k,v)| hash[k.to_sym] = v; hash}
      end
    end
  end
end
Date.send(:include, DateExt)

spec = "#{test_dir}/mri/date/test_date_strptime.rb" # Ported from test/mri/date/test_date_strptime.rb in JRuby
exit Test::Unit::AutoRunner.run(true, spec)
