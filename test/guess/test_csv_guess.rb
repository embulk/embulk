require 'helper'
require 'time'
require 'embulk/guess/csv'

class CsvGuessTest < ::Test::Unit::TestCase
  class TestDelimiter < self
    data(
      "\t" => "\t",
      "," => ",",
      "|" => "|",
    )
    def test_delimiter_detection(delim)
      actual = guess([
        ["1", "foo"].join(delim),
        ["2", "bar"].join(delim),
      ])
      assert_equal delim, actual["parser"]["delimiter"]
    end
  end

  class TestQuote < self
    data(
      "'" => "'",
      '"' => '"',
      nil => nil,
    )
    def test_quote(quotation)
      actual = guess([
        %w(1 foo).map{|str| %Q(#{quotation}#{str}#{quotation})}.join("\t"),
        %w(2 bar).map{|str| %Q(#{quotation}#{str}#{quotation})}.join("\t"),
      ])
      assert_equal quotation, actual["parser"]["quote"]
    end
  end

  class TestEscape < self
    data(
      "\\" => "\\",
      '"' => '"',
    )
    def test_escape(char)
      actual = guess([
        %Q('1'\t'F#{char}'OO'),
        %Q('2'\t'FOOOOOOOO#{char}'OO'),
      ])
      assert_equal char, actual["parser"]["escape"]
    end
  end

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

  class TestTrim < self
    def test_trim_flag_when_will_be_long_if_strip_arround_space
      actual = guess([
        "  1 \tfoo",
        "  2 \tfoo",
        "  3 \tfoo",
      ])
      assert_equal true, actual["parser"]["trim_if_not_quoted"]
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
    def test_columns_without_header
      actual = guess([
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "c0", "type" => "long"},
        {"name" => "c1", "type" => "string"},
        {"name" => "c2", "type" => "timestamp", "format"=>"%Y-%m-%dT%H:%M:%S%z"},
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
        {"name" => "num", "type" => "long"},
        {"name" => "str", "type" => "string"},
        {"name" => "time", "type" => "timestamp", "format"=>"%Y-%m-%dT%H:%M:%S%z"},
      ]
      assert_equal expected, actual["parser"]["columns"]
    end

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
