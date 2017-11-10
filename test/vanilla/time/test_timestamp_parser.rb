require 'helper'
require 'date'
require 'time'

# TimestampParserTest test org.embulk.spi.time.TimestampParser by comparing with Ruby's DateTime.strptime.
#
# Some test cases are imported from Ruby v2.3.1's test/date/test_date_strptime.rb. See its COPYING for license.
#
# *{test/date/test_date_strptime.rb}[https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/test/date/test_date_strptime.rb?view=markup]
# *{COPYING}[https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup]

class TimestampParserTest < ::Test::Unit::TestCase
  def test__strptime__3
    [
      # iso8601
      ['2001-02-03', '%Y-%m-%d'],
      ['2001-02-03T23:59:60', '%Y-%m-%dT%H:%M:%S'],
      ['2001-02-03T23:59:60+09:00', '%Y-%m-%dT%H:%M:%S%Z'],
      ['-2001-02-03T23:59:60+09:00', '%Y-%m-%dT%H:%M:%S%Z'],
      ['+012345-02-03T23:59:60+09:00', '%Y-%m-%dT%H:%M:%S%Z'],
      ['-012345-02-03T23:59:60+09:00', '%Y-%m-%dT%H:%M:%S%Z'],

      # ctime(3), asctime(3)
      ['Thu Jul 29 14:47:19 1999', '%c'],
      ['Thu Jul 29 14:47:19 -1999', '%c'],

      # date(1)
      ['Thu Jul 29 16:39:41 EST 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 MET DST 1999', '%a %b %d %H:%M:%S %Z %Y'],
      # Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "AST" is used here instead.
      # "AMT" is not recognized even by Ruby v2.3.1's zonetab.
      ['Thu Jul 29 16:39:41 AST 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 AST -1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT+09 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT+0908 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT+090807 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT-09 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT-09:08 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT-09:08:07 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT-3.5 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 GMT-3,5 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 Mountain Daylight Time 1999', '%a %b %d %H:%M:%S %Z %Y'],
      ['Thu Jul 29 16:39:41 E. Australia Standard Time 1999', '%a %b %d %H:%M:%S %Z %Y'],

      # rfc822
      ['Thu, 29 Jul 1999 09:54:21 UT', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul 1999 09:54:21 GMT', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul 1999 09:54:21 PDT', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul 1999 09:54:21 z', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul 1999 09:54:21 +0900', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul 1999 09:54:21 +0430', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul 1999 09:54:21 -0430', '%a, %d %b %Y %H:%M:%S %Z'],
      ['Thu, 29 Jul -1999 09:54:21 -0430', '%a, %d %b %Y %H:%M:%S %Z'],

      # etc
      ['06-DEC-99', '%d-%b-%y'],
      ['sUnDay oCtoBer 31 01', '%A %B %d %y'],
      ["October\t\n\v\f\r 15,\t\n\v\f\r99", '%B %d, %y'],
      ["October\t\n\v\f\r 15,\t\n\v\f\r99", '%B%t%d,%n%y'],

      ['09:02:11 AM', '%I:%M:%S %p'],
      ['09:02:11 A.M.', '%I:%M:%S %p'],
      ['09:02:11 PM', '%I:%M:%S %p'],
      ['09:02:11 P.M.', '%I:%M:%S %p'],

      ['12:33:44 AM', '%r'],
      ['01:33:44 AM', '%r'],
      ['11:33:44 AM', '%r'],
      ['12:33:44 PM', '%r'],
      ['01:33:44 PM', '%r'],
      ['11:33:44 PM', '%r'],

      # Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "AST" is used here instead.
      # "AMT" is not recognized even by Ruby v2.3.1's zonetab.
      ['11:33:44 PM AST', '%I:%M:%S %p %Z'],
      ['11:33:44 P.M. AST', '%I:%M:%S %p %Z'],

      ['fri1feb034pm+5', '%a%d%b%y%H%p%Z'],
    ].each do |string, format|
      before_date = Date.today
      after_date = nil
      while before_date != after_date do
        before_date = Date.today
        expected_datetime = DateTime.strptime(string, format)
        expected_time = expected_datetime.new_offset(0).to_time.utc
        after_date = Date.today
        if expected_datetime.to_date == after_date  # DateTime.strptime fills today's date if date is not contained.
          # Getting the Time of 00:00:00 UTC on after_date.
          expected_time = expected_time - after_date.to_datetime.new_offset(0).to_time.utc
        end
        expected_epoch = expected_time.to_i
      end

      timestamp_parser =
        Java::org.embulk.spi.time.TimestampParser.new(format, Java::org.joda.time.DateTimeZone::UTC, "1970-01-01")
      actual_timestamp = timestamp_parser.parse(string)
      actual_epoch = actual_timestamp.getEpochSecond()

      assert_equal(expected_epoch, actual_epoch, string)
    end
  end
end
