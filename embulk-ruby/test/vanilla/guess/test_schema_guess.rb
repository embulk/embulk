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

  def test_boolean
    %w[
      true false t f
      yes no y n
      on off
    ].each do |str|
      # If at least one of three kinds of boolean strings (i.e., downcase, upcase, capitalize) is
      # mistakenly detected as "string," the guesser concludes the column type is "string."
      assert_equal(
        [C.new(0, "a", :boolean)],
        G.from_hash_records([
          {"a" => str.downcase},
          {"a" => str.upcase},
          {"a" => str.capitalize},
        ]))
    end
  end
end
