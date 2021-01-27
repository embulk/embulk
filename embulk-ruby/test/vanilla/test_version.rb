require 'embulk/version'

class VersionTest < ::Test::Unit::TestCase
  def test_version
    STDERR.puts ""
    STDERR.puts "CORE_VERSION: #{Java::org.embulk.EmbulkVersion::VERSION}"
    STDERR.puts "GEM_VERSION: #{::Embulk::VERSION}"
    assert_equal(::String.new(Java::org.embulk.EmbulkVersion::VERSION).tr('-', '.').downcase, ::Embulk::VERSION)
  end
end
