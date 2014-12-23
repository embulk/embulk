module Embulk
  module Java
    #require 'embulk/java/imports'
    require 'time'  # Date._strptime

    class TimeParserHelper
      #include Java::JRubyTimeParserHelper
      include org.embulk.time.JRubyTimeParserHelper

      class Factory
        #include Java::JRubyTimeParserHelperFactory
        include org.embulk.time.JRubyTimeParserHelperFactory

        def newInstance(format_string, year, mon, day, hour, min, sec, usec)
          default_time = Time.utc(year, mon, day, hour, min, sec, usec)
          TimeParserHelper.new(format_string, default_time)
        end
      end

      def initialize(format_string, default_time)
        @format_string = format_string
        @default_time = default_time
      end

      # Override
      def strptime(text)
        hash = Date._strptime(text, @format_string)
        unless hash
          raise Java::TimestampParseException.new
        end

        if seconds = hash[:seconds]
          return seconds * 1_000_000

        else
          year = hash[:year]
          mon = hash[:mon]
          day = hash[:mday]
          hour = hash[:hour]
          min = hash[:min]
          sec = hash[:sec]
          sec_fraction = hash[:sec_fraction]
          zone = hash[:zone]
          usec = hash[:sec_fraction] ? hash[:sec_fraction] * 1000000 : nil

          now = @default_time
          begin
            break if year; year = now.year
            break if mon; mon = now.mon
            break if day; day = now.day
            break if hour; hour = now.hour
            break if min; min = now.min
            break if sec; sec = now.sec
            break if sec_fraction; usec = now.tv_usec
          end until true

          year ||= 1970
          mon ||= 1
          day ||= 1
          hour ||= 0
          min ||= 0
          sec ||= 0
          usec ||= 0

          @zone = zone
          time = Time.utc(year, mon, day, hour, min, sec, usec)
          return time.tv_sec * 1000 + time.tv_usec / 1000
        end
      end

      # Override
      def getZone
        @zone
      end
    end
  end
end
