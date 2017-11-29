require 'helper'
require 'time'
require 'embulk/guess/csv_all_strings'

class CsvAllStringsGuessTest < ::Test::Unit::TestCase
  class TestAllStrings < self
    def test_columns_without_header
      actual = guess([
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "c0", "type" => "string"},
        {"name" => "c1", "type" => "string"},
        {"name" => "c2", "type" => "string"},
      ]
      assert_equal expected, actual["parser"]["columns"]
    end

    def test_columns_with_header
      actual = guess([
        "num\tstr\ttime",
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "num", "type" => "string"},
        {"name" => "str", "type" => "string"},
        {"name" => "time", "type" => "string"},
      ]
      assert_equal expected, actual["parser"]["columns"]
    end
  end

  def guess(texts)
    conf = Embulk::DataSource.new({
      parser: {
        type: "csv"
      }
    })
    Embulk::Guess::CsvAllStringsGuessPlugin.new.guess_lines(conf, Array(texts))
  end
end
