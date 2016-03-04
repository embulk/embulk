module Embulk::Guess
  module TimeFormatGuess
    module Parts
      YEAR = /[1-4][0-9]{3}/
      MONTH         = /10|11|12|[0 ]?[0-9]/
      MONTH_NODELIM = /10|11|12|[0][0-9]/
      DAY         = /31|30|[1-2][0-9]|[0 ]?[1-9]/
      DAY_NODELIM = /31|30|[1-2][0-9]|[0][1-9]/
      HOUR         = /20|21|22|23|24|1[0-9]|[0 ]?[0-9]/
      HOUR_NODELIM = /20|21|22|23|24|1[0-9]|[0][0-9]/
      MINUTE         = SECOND         = /60|[1-5][0-9]|[0 ]?[0-9]/
      MINUTE_NODELIM = SECOND_NODELIM = /60|[1-5][0-9]|[0][0-9]/

      MONTH_NAME_SHORT = /Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec/
      MONTH_NAME_FULL = /January|February|March|April|May|June|July|August|September|October|November|December/

      WEEKDAY_NAME_SHORT = /Sun|Mon|Tue|Wed|Thu|Fri|Sat/
      WEEKDAY_NAME_FULL = /Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday/

      ZONE_OFF = /(?:Z|[\-\+]\d\d(?::?\d\d)?)/
      ZONE_ABB = /[A-Z]{1,3}/
    end

    class GuessMatch
      def initialize(delimiters, parts, part_options)
        @delimiters = delimiters
        @parts = parts
        @part_options = part_options
      end

      def format
        format = ''
        @parts.size.times do |i|
          format << @delimiters[i-1] if i != 0
          option = @part_options[i]

          case @parts[i]
          when :year
            format << '%Y'

          when :month
            case option
            when :zero
              format << '%m'
            when :blank
              #format << '%_m'  # not supported
              format << '%m'
            when :none
              #format << '%-m'  # not supported
              format << '%m'
            else
              format << '%m'
            end

          when :day
            case option
            when :zero
              format << '%d'
            when :blank
              format << '%e'
            when :none
              format << '%d'  # not supported
            else
              format << '%d'
            end

          when :hour
            case option
            when :zero
              format << '%H'
            when :blank
              format << '%k'
            when :none
              format << '%k'  # not supported
            else
              format << '%H'
            end

          when :minute
            # heading options are not supported
            format << '%M'

          when :second
            # heading options are not supported
            format << '%S'

          when :frac
            if option <= 3
              format << '%L'
            #elsif option <= 6
            #  format << '%6N'
            #elsif option <= 6
            #  format << '%6N'
            #elsif option <= 9
            #  format << '%9N'
            #elsif option <= 12
            #  format << '%12N'
            #elsif option <= 15
            #  format << '%15N'
            #elsif option <= 18
            #  format << '%18N'
            #elsif option <= 21
            #  format << '%21N'
            #elsif option <= 24
            #  format << '%24N'
            else
              format << '%N'
            end

          when :zone
            case option
            when :extended
              format << '%:z'
            else
              # :simple, :abb
              # don't use %Z even with :abb: https://github.com/jruby/jruby/issues/3702
              format << '%z'
            end

          else
            raise "Unknown part: #{@parts[i]}"
          end
        end

        return format
      end

      def mergeable_group
        # MDY is mergible with DMY
        if i = array_sequence_find(@parts, [:day, :month, :year])
          ps = @parts.dup
          ps[i, 3] = [:month, :day, :year]
          [@delimiters, ps]
        else
          [@delimiters, @parts]
        end
      end

      attr_reader :parts
      attr_reader :part_options

      def merge!(another_in_group)
        part_options = another_in_group.part_options
        @part_options.size.times do |i|
          @part_options[i] ||= part_options[i]
          if @part_options[i] == nil
            part_options[i]
          elsif part_options[i] == nil
            @part_options[i]
          else
            [@part_options[i], part_options[i]].sort.last
          end
        end

        # if DMY matches, MDY is likely false match of DMY.
        dmy = array_sequence_find(another_in_group.parts, [:day, :month, :year])
        mdy = array_sequence_find(@parts, [:month, :day, :year])
        if mdy && dmy
          @parts[mdy, 3] = [:day, :month, :year]
        end
      end

      def array_sequence_find(array, seq)
        (array.size - seq.size + 1).times {|i|
          return i if array[i, seq.size] == seq
        }
        return nil
      end
    end

    class GuessPattern
      include Parts

      date_delims = /[\/\-\.]/
      # yyyy-MM-dd
      YMD         = /(?<year>#{YEAR})(?<date_delim>#{date_delims})(?<month>#{MONTH})\k<date_delim>(?<day>#{DAY})/
      YMD_NODELIM = /(?<year>#{YEAR})(?<month>#{MONTH_NODELIM})(?<day>#{DAY_NODELIM})/
      # MM/dd/yyyy
      MDY         = /(?<month>#{MONTH})(?<date_delim>#{date_delims})(?<day>#{DAY})\k<date_delim>(?<year>#{YEAR})/
      MDY_NODELIM = /(?<month>#{MONTH_NODELIM})(?<day>#{DAY_NODELIM})(?<year>#{YEAR})/
      # dd.MM.yyyy
      DMY         = /(?<day>#{DAY})(?<date_delim>#{date_delims})(?<month>#{MONTH})\k<date_delim>(?<year>#{YEAR})/
      DMY_NODELIM = /(?<day>#{DAY_NODELIM})(?<month>#{MONTH_NODELIM})(?<year>#{YEAR})/

      frac = /[0-9]{1,9}/
      time_delims = /[\:\-]/
      frac_delims = /[\.\,]/
      TIME         = /(?<hour>#{HOUR})(?:(?<time_delim>#{time_delims})(?<minute>#{MINUTE})(?:\k<time_delim>(?<second>#{SECOND})(?:(?<frac_delim>#{frac_delims})(?<frac>#{frac}))?)?)?/
      TIME_NODELIM = /(?<hour>#{HOUR_NODELIM})(?:(?<minute>#{MINUTE_NODELIM})((?<second>#{SECOND_NODELIM})(?:(?<frac_delim>#{frac_delims})(?<frac>#{frac}))?)?)?/

      ZONE = /(?<zone_space> )?(?<zone>(?<zone_off>#{ZONE_OFF})|(?<zone_abb>#{ZONE_ABB}))/

      def match(text)
        delimiters = []
        parts = []
        part_options = []

        if dm = (/^#{YMD}(?<rest>.*?)$/.match(text) or /^#{YMD_NODELIM}(?<rest>.*?)$/.match(text))
          date_delim = dm["date_delim"] rescue ""

          parts << :year
          part_options << nil

          delimiters << date_delim
          parts << :month
          part_options << part_heading_option(dm["month"])

          delimiters << date_delim
          parts << :day
          part_options << part_heading_option(dm["day"])

        elsif dm = (/^#{MDY}(?<rest>.*?)$/.match(text) or /^#{MDY_NODELIM}(?<rest>.*?)$/.match(text))
          date_delim = dm["date_delim"] rescue ""

          parts << :month
          part_options << part_heading_option(dm["month"])

          delimiters << date_delim
          parts << :day
          part_options << part_heading_option(dm["day"])

          delimiters << date_delim
          parts << :year
          part_options << nil

        elsif dm = (/^#{DMY}(?<rest>.*?)$/.match(text) or /^#{DMY_NODELIM}(?<rest>.*?)$/.match(text))
          date_delim = dm["date_delim"] rescue ""

          parts << :day
          part_options << part_heading_option(dm["day"])

          delimiters << date_delim
          parts << :month
          part_options << part_heading_option(dm["month"])

          delimiters << date_delim
          parts << :year
          part_options << nil

        else
          date_delim = ""
          return nil
        end
        rest = dm["rest"]

        date_time_delims = /(:? |_|T|\. ?)/
        if tm = (
              /^(?<date_time_delim>#{date_time_delims})#{TIME}(?<rest>.*?)?$/.match(rest) or
              /^(?<date_time_delim>#{date_time_delims})#{TIME_NODELIM}(?<rest>.*?)?$/.match(rest) or
              (date_delim == "" && /^#{TIME_NODELIM}(?<rest>.*?)?$/.match(rest))
            )
          date_time_delim = tm["date_time_delim"] rescue ""
          time_delim = tm["time_delim"] rescue ""

          delimiters << date_time_delim
          parts << :hour
          part_options << part_heading_option(tm["hour"])

          if tm["minute"]
            delimiters << time_delim
            parts << :minute
            part_options << part_heading_option(tm["minute"])

            if tm["second"]
              delimiters << time_delim
              parts << :second
              part_options << part_heading_option(tm["second"])

              if tm["frac"]
                delimiters << tm["frac_delim"]
                parts << :frac
                part_options << tm["frac"].size
              end
            end
          end

          rest = tm["rest"]
        end

        if zm = /^#{ZONE}$/.match(rest)
          delimiters << (zm["zone_space"] || '')
          parts << :zone
          if zm["zone_off"]
            if zm["zone_off"].include?(':')
              part_options << :extended
            else
              part_options << :simple
            end
          else
            part_options << :abb
          end

          return GuessMatch.new(delimiters, parts, part_options)

        elsif rest =~ /^\s*$/
          return GuessMatch.new(delimiters, parts, part_options)

        else
          return nil
        end
      end

      def part_heading_option(text)
        if text[0] == '0'
          :zero
        elsif text[0] == ' '
          :blank
        elsif text.size == 1
          :none
        else
          nil
        end
      end
    end

    class SimpleMatch
      def initialize(format)
        @format = format
      end

      attr_reader :format

      def mergeable_group
        @format
      end

      def merge!(another_in_group)
      end
    end

    class Rfc2822Pattern
      include Parts

      def initialize
        @regexp = /^(?<weekday>#{WEEKDAY_NAME_SHORT}, )?\d\d #{MONTH_NAME_SHORT} \d\d\d\d(?<time> \d\d:\d\d(?<second>:\d\d)? (?:(?<zone_off>#{ZONE_OFF})|(?<zone_abb>#{ZONE_ABB})))?$/
      end

      def match(text)
        if m = @regexp.match(text)
          format = ''
          format << "%a, " if m['weekday']
          format << "%d %b %Y"
          format << " %H:%M" if m['time']
          format << ":%S" if m['second']
          if m['zone_off']
            if m['zone_off'].include?(':')
              format << " %:z"
            else
              format << " %z"
            end
          elsif m['zone_abb']
            # don't use %Z: https://github.com/jruby/jruby/issues/3702
            format << " %z" if m['zone_abb']
          end
          SimpleMatch.new(format)
        else
          nil
        end
      end
    end

    class RegexpPattern
      def initialize(regexp, format)
        @regexp = regexp
        @match = SimpleMatch.new(format)
      end

      def match(text)
        if @regexp =~ text
          return @match
        else
          return nil
        end
      end
    end

    module StandardPatterns
      include Parts

      APACHE_CLF = /^\d\d\/#{MONTH_NAME_SHORT}\/\d\d\d\d:\d\d:\d\d:\d\d #{ZONE_OFF}?$/
      ANSI_C_ASCTIME = /^#{WEEKDAY_NAME_SHORT} #{MONTH_NAME_SHORT} \d\d? \d\d:\d\d:\d\d \d\d\d\d$/
    end

    PATTERNS = [
      GuessPattern.new,
      Rfc2822Pattern.new,
      RegexpPattern.new(StandardPatterns::APACHE_CLF, "%d/%b/%Y:%H:%M:%S %z"),
      RegexpPattern.new(StandardPatterns::ANSI_C_ASCTIME, "%a %b %e %H:%M:%S %Y"),
    ]

    def self.guess(texts)
      texts = Array(texts).map {|text| text.to_s }
      texts.reject! {|text| text == "" }
      matches = texts.map do |text|
        PATTERNS.map {|pattern| pattern.match(text) }.compact
      end.flatten
      if matches.empty?
        return nil
      elsif matches.size == 1
        return matches[0].format
      else
        match_groups = matches.group_by {|match| match.mergeable_group }.values
        best_match_group = match_groups.sort_by {|group| group.size }.last
        best_match = best_match_group.shift
        best_match_group.each {|m| best_match.merge!(m) }
        return best_match.format
      end
    end
  end
end
