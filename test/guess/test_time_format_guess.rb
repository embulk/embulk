require 'helper'
require 'time'
require 'embulk/guess/time_format_guess'

class TimeFormatGuessTest < ::Test::Unit::TestCase
  def test_format_delims
    # date-delim "-"  date-time-delim " "  time-delim ":"  frac delim "."
    assert_guess "%Y-%m-%d %H:%M:%S.%N",    "2014-01-01 01:01:01.000000001"
    assert_guess "%Y-%m-%d %H:%M:%S.%N",    "2014-01-01 01:01:01.000001"
    assert_guess "%Y-%m-%d %H:%M:%S.%L",    "2014-01-01 01:01:01.001"
    assert_guess "%Y-%m-%d %H:%M:%S",       "2014-01-01 01:01:01"
    assert_guess "%Y-%m-%d %H:%M",          "2014-01-01 01:01"
    assert_guess "%Y-%m-%d",                "2014-01-01"

    # date-delim "/"  date-time-delim " "  time-delim "-"  frac delim ","
    assert_guess "%Y/%m/%d %H-%M-%S,%N",    "2014/01/01 01-01-01,000000001"
    assert_guess "%Y/%m/%d %H-%M-%S,%N",    "2014/01/01 01-01-01,000001"
    assert_guess "%Y/%m/%d %H-%M-%S,%L",    "2014/01/01 01-01-01,001"
    assert_guess "%Y/%m/%d %H-%M-%S",       "2014/01/01 01-01-01"
    assert_guess "%Y/%m/%d %H-%M",          "2014/01/01 01-01"
    assert_guess "%Y/%m/%d",                "2014/01/01"

    # date-delim "."  date-time-delim "."  time-delim ":"  frac delim "."
    assert_guess "%Y.%m.%d.%H:%M:%S.%N",    "2014.01.01.01:01:01.000000001"
    assert_guess "%Y.%m.%d.%H:%M:%S.%N",    "2014.01.01.01:01:01.000001"
    assert_guess "%Y.%m.%d.%H:%M:%S.%L",    "2014.01.01.01:01:01.001"
    assert_guess "%Y.%m.%d.%H:%M:%S",       "2014.01.01.01:01:01"
    assert_guess "%Y.%m.%d.%H:%M",          "2014.01.01.01:01"
    assert_guess "%Y.%m.%d",                "2014.01.01"

    # date-delim "."  date-time-delim ". "  time-delim ":"  frac delim ","
    assert_guess "%Y.%m.%d. %H:%M:%S,%N",    "2014.01.01. 01:01:01,000000001"
    assert_guess "%Y.%m.%d. %H:%M:%S,%N",    "2014.01.01. 01:01:01,000001"
    assert_guess "%Y.%m.%d. %H:%M:%S,%L",    "2014.01.01. 01:01:01,001"
    assert_guess "%Y.%m.%d. %H:%M:%S",       "2014.01.01. 01:01:01"
    assert_guess "%Y.%m.%d. %H:%M",          "2014.01.01. 01:01"
    assert_guess "%Y.%m.%d",                 "2014.01.01"
  end

  def test_format_ymd_orders
    assert_guess "%Y-%m-%d", "2014-01-01"
    assert_guess "%Y/%m/%d", "2014/01/01"
    assert_guess "%Y.%m.%d", "2014.01.01"
    assert_guess "%m/%d/%Y", "01/01/2014"
    assert_guess "%m.%d.%Y", "01.01.2014"
    assert_guess "%d/%m/%Y", "13/01/2014"
    assert_guess "%d/%m/%Y", "21/01/2014"

    assert_guess "%d/%m/%Y %H-%M-%S,%N",    "21/01/2014 01-01-01,000000001"
    assert_guess "%d/%m/%Y %H-%M-%S,%N",    "21/01/2014 01-01-01,000001"
    assert_guess "%d/%m/%Y %H-%M-%S,%L",    "21/01/2014 01-01-01,001"
    assert_guess "%d/%m/%Y %H-%M-%S",       "21/01/2014 01-01-01"
    assert_guess "%d/%m/%Y %H-%M",          "21/01/2014 01-01"
    assert_guess "%d/%m/%Y",                "21/01/2014"
  end

  def test_format_borders
    assert_guess "%Y-%m-%d %H:%M:%S.%N",    "2014-12-31 23:59:59.999999999"
  end

  def test_format_iso8601
    assert_guess "%Y-%m-%d", "1981-04-05"
    assert_guess "%Y-%m-%dT%H", "2007-04-06T13"
    assert_guess "%Y-%m-%dT%H:%M", "2007-04-06T00:00"
    assert_guess "%Y-%m-%dT%H:%M", "2007-04-05T24:00"
    assert_guess "%Y-%m-%dT%H:%M:%S", "2007-04-06T13:47:30"
    assert_guess "%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30Z"
    assert_guess "%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30+00"
    assert_guess "%Y-%m-%dT%H:%M:%S%:z", "2007-04-06T13:47:30+00:00"
    assert_guess "%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30+0000"
    assert_guess "%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30-01"
    assert_guess "%Y-%m-%dT%H:%M:%S%:z", "2007-04-06T13:47:30-01:30"
    assert_guess "%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30-0130"
  end

  def test_format_rfc_822_2822
    # This test is disabled because of https://github.com/jruby/jruby/issues/3702
    #assert_guess '%a, %d %b %Y %H:%M:%S %Z', "Fri, 20 Feb 2015 14:02:34 PST"
    assert_guess '%a, %d %b %Y %H:%M:%S %z', "Fri, 20 Feb 2015 22:02:34 UT"
    assert_guess '%a, %d %b %Y %H:%M:%S %z', "Fri, 20 Feb 2015 22:02:34 GMT"
    assert_guess     '%d %b %Y %H:%M:%S %z',      "20 Feb 2015 22:02:34 GMT"
    assert_guess     '%d %b %Y %H:%M %z',         "20 Feb 2015 22:02 GMT"
    assert_guess '%a, %d %b %Y %H:%M %z',    "Fri, 20 Feb 2015 22:02 GMT"
    assert_guess     '%d %b %Y',                  "20 Feb 2015"
    assert_guess '%a, %d %b %Y',             "Fri, 20 Feb 2015"
    assert_guess '%a, %d %b %Y %H:%M %z',    "Fri, 20 Feb 2015 22:02 +0000"
    assert_guess '%a, %d %b %Y %H:%M %:z',   "Fri, 20 Feb 2015 22:02 +00:00"
    assert_guess '%a, %d %b %Y %H:%M %z',    "Fri, 20 Feb 2015 22:02 +00"
  end

  def test_format_apache_clf
    assert_guess '%d/%b/%Y:%H:%M:%S %z', "07/Mar/2004:16:05:50 -0800"
  end

  def test_format_ansi_c_asctime
    assert_guess '%a %b %e %H:%M:%S %Y', "Fri May 11 21:44:53 2001"
  end

  def test_format_merge_frequency
    assert_guess_partial 2, "%Y-%m-%d %H:%M:%S", ["2014-01-01", "2014-01-01 00:00:00", "2014-01-01 00:00:00"]
    assert_guess_partial 3, "%Y-%m-%d %H:%M:%S %z", ["2014-01-01 00:00:00 +0000", "2014-01-01 00:00:00 +0000", "2014-01-01 00:00:00 +00:00"]
  end

  def test_format_merge_dmy
    # DMY has higher priority than MDY
    assert_guess "%m/%d/%Y", ["01/01/2014"]
    assert_guess "%d/%m/%Y", ["01/01/2014", "01/01/2014", "13/01/2014"]
    assert_guess "%d.%m.%Y", ["01.01.2014", "01.01.2014", "13.01.2014"]
    # but frequency is more important if delimiter is different
    assert_guess_partial 2, "%m/%d/%Y", ["01/01/2014", "01/01/2014", "13.01.2014"]
  end

  def assert_guess(format, texts)
    assert_equal format, guess(texts)
    Array(texts).each do |text|
      time = Time.strptime(text, format)
      assert_equal time.to_i, Time.strptime(time.strftime(format), format).to_i
    end
  end

  def assert_guess_partial(count, format, texts)
    assert_equal format, guess(texts)
    times = Array(texts).map do |text|
      Time.strptime(text, format) rescue nil
    end.compact
    assert_equal count, times.size
    times.each do |time|
      assert_equal time.to_i, Time.strptime(time.strftime(format), format).to_i
    end
  end

  def guess(texts)
    Embulk::Guess::TimeFormatGuess.guess(Array(texts))
  end
end
