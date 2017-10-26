base_dir = File.expand_path(File.join(File.dirname(__FILE__), "..", ".."))
lib_dir = File.join(base_dir, "lib")
test_dir = File.join(base_dir, "test")

$LOAD_PATH.unshift(lib_dir)
$LOAD_PATH.unshift(test_dir)

require "helper"
require "date"
require "test/unit"

module DateExt
  def self.included base
    base.instance_eval do
      def _strptime(str, fmt='%F')
        parser = org.embulk.spi.time.RubyDateParser.new
        map = parser.parse(JRuby.runtime.current_context, fmt, str)
        return map.nil? ? nil : map.to_hash.inject({}){|hash,(k,v)| hash[k.to_sym] = v; hash}
      end
    end
  end
end
Date.send(:include, DateExt)

Dir.glob("#{base_dir}/test/monkey_strptime/**/test{_,-}*.rb") do |file|
  require file.sub(/\.rb$/,"")
end

exit Test::Unit::AutoRunner.run
