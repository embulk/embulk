require "test/unit"

base_dir = File.expand_path(File.join(File.dirname(__FILE__), "../../../../"))

class JsonTest < ::Test::Unit::TestCase
  def test_foo
    assert_equal 1, 1
  end

  def test_bar
    assert_equal 1, 2  # this test fails
  end
end

Test::Unit::AutoRunner.run
