require 'helper'
require 'time'
require 'embulk/guess/schema_guess'

class SchemaGuessTest < ::Test::Unit::TestCase
  G = Embulk::Guess::SchemaGuess
  C = Embulk::Column

  def test_guess
    G.from_hash_records([{"int" => "1", "str" => "a"}])
  end

  def test_coalesce
    assert_equal(
      [C.new(0, "a", :timestamp, "%Y%m%d")],
      G.from_hash_records([
        {"a" => "20160101"},
        {"a" => "20160101"},
      ]))

    assert_equal(
      [C.new(0, "a", :long)],
      G.from_hash_records([
        {"a" => "20160101"},
        {"a" => "20160101"},
        {"a" => "12345678"},
      ]))
  end
end
