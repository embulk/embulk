require 'helper'
require 'time'
require 'embulk/guess/schema_guess'

class SchemaGuessTest < ::Test::Unit::TestCase
  G = Embulk::Guess::SchemaGuess

  def test_guess
    G.schema_from_hash([{"int" => "1", "str" => "a"}])
  end
end
