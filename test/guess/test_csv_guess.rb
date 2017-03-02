require 'helper'
require 'time'
require 'embulk/guess/csv'

class CsvGuessTest < ::Test::Unit::TestCase
  class TestSkipHeaderLines < self
    def test_skip_header_lines_one
      actual = guess([
        "col1\tcol2",
        "1\tfoo",
        "2\tbar",
      ])
      assert_equal 1, actual["parser"]["skip_header_lines"]
    end

    def test_skip_header_lines_three
      actual = guess([
        "this is a CSV",
        "created for a test",
        "col1\tcol2",
        "1\tfoo",
        "2\tbar",
      ])
      assert_equal 3, actual["parser"]["skip_header_lines"]
    end
  end

  class TestNullString < self
    data(
      "\\N" => "\\N",
      "null" => "null",
      "NULL" => "NULL",
      "#N/A" => "#N/A",
      nil => nil,
    )
    def test_null_string(null)
      actual = guess([
        "1\tfoo\t#{null}",
        "2\tbar\t#{null}",
      ])
      assert_equal null, actual["parser"]["null_string"]
    end
  end

  class TestCommentLineMarker < self
    data(
      "#" => "#",
      "//" => "//",
    )
    def test_comment_line_marker(marker)
      actual = guess([
        "foo\t 1\tother",
        "#{marker} foo\t 2\tother",
        "foo\t 3\tother",
      ])
      assert_equal marker, actual["parser"]["comment_line_marker"]
    end
  end

  class TestColumns < self
    def test_complex_line
      actual = guess([
        %Q(this is useless header),
        %Q(and more),
        %Q(num,str,quoted_num,time),
        %Q(1, "value with space "" and quote in it", "123",21150312000000Z),
        %Q(2),
        %Q(# 3, "this is commented out" ,"1",21150312000000Z),
      ])
      expected = [
        {"name" => "num", "type" => "long"},
        {"name" => "str", "type" => "string"},
        {"name" => "quoted_num", "type" => "long"},
        {"name" => "time", "type" => "timestamp", "format"=>"%Y%m%d%H%M%S%z"},
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
    Embulk::Guess::CsvGuessPlugin.new.guess_lines(conf, Array(texts))
  end
end
