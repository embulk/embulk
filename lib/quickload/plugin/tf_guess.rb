module QuickLoad::Plugin::TFGuess
  # time format guess

  module Parts
    YEAR = /[1-4][0-9]{3}/
    MONTH = /[0 ]?[0-9]|10|11|12/
    DAY = /[0 ]?[1-9]|[1-2][0-9]|30|31/
    HOUR = /[0 ]?[0-9]|20|21|22|23|24/
    MINUTE = SECOND = /[0 ][0-9]|[1-5]?[0-9]|60/
  end

  class GuessMatch
    def initialize(delimiters, parts, headings)
      @delimiters = delimiters
      @parts = parts
      @headings = headings
    end

    def format
      format = ''
      @parts.size.times do |i|
        format << @delimiters[i-1] if i != 0
        heading = @headings[i]

        case @parts[i]
        when :year
          format << '%Y'

        when :month
          case heading
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
          case heading
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
          case heading
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
          if heading <= 3
            format << '%L'
          #elsif heading <= 6
          #  format << '%6N'
          #elsif heading <= 6
          #  format << '%6N'
          #elsif heading <= 9
          #  format << '%9N'
          #elsif heading <= 12
          #  format << '%12N'
          #elsif heading <= 15
          #  format << '%15N'
          #elsif heading <= 18
          #  format << '%18N'
          #elsif heading <= 21
          #  format << '%21N'
          #elsif heading <= 24
          #  format << '%24N'
          else
            format << '%N'
          end

        when :zone_off
          format << '%z'

        when :zone_abb
          format << '%Z'

        else
          raise "Unknown part: #{@parts[i]}"
        end
      end

      return format
    end

    def mergeable_group
      [@delimiters, @parts]
    end

    attr_reader :headings

    def merge!(another_in_group)
      headings = another_in_group.headings
      @headings.size.times do |i|
        @headings[i] ||= headings[i]
        if @headings[i] != headings[i]
          @headings[i] = :zero
        end
      end
    end
  end

  class GuessPattern
    include Parts

    date_delims = /[\/\-]/
    YMD = /(?<year>#{YEAR})(?<date_delim>#{date_delims}?)(?<month>#{MONTH})\k<date_delim>(?<day>#{DAY})/  # yyyy-MM-dd
    DMY = /(?<year>#{YEAR})(?<date_delim>\/)(?<month>#{MONTH})\k<date_delim>(?<day>#{DAY})/  # dd/MM/yyyy

    frac = /[0-9]{1,24}/
    time_delims = /[\:\-]/
    frac_delims = /[\.\,]/
    TIME = /(?<hour>#{HOUR})(?<time_delim>#{time_delims}?)(?<minute>#{MINUTE})(?:\k<time_delim>(?<second>#{SECOND})(?:(?<frac_delim>#{frac_delims})(?<frac>#{frac}))?)?/

    TZ = /(?<zone_space> )?(?<zone>(?<zone_off>[\-\+]\d\d(?::?\d\d)?)|(?<zone_abb>[A-Z]{3}))|(?<z>Z)/

    def match(text)
      delimiters = []
      parts = []
      headings = []

      if dm = /^#{YMD}(?<rest>.*)$/.match(text)
        parts << :year
        headings << nil
        delimiters << dm["date_delim"]

        parts << :month
        headings << part_heading(dm["month"])
        delimiters << dm["date_delim"]

        parts << :day
        headings << part_heading(dm["day"])

      elsif dm = /^#{DMY}(?<rest>.*)$/.match(text)
        parts << :day
        headings << part_heading(dm["day"])
        delimiters << dm["date_delim"]

        parts << :month
        headings << part_heading(dm["month"])
        delimiters << dm["date_delim"]

        parts << :year
        headings << nil
        delimiters << dm["date_delim"]

      else
        return nil
      end
      rest = dm["rest"]

      if dm["date_delim"] == ""
        date_time_delims = /(?<date_time_delim>[ T]?)/
      else
        date_time_delims = /(?<date_time_delim>[ T])/
      end

      if tm = /^#{date_time_delims}#{TIME}(?<rest>.*)?$/.match(rest)
        delimiters << tm["date_time_delim"]
        parts << :hour
        headings << part_heading(tm["hour"])

        delimiters << tm["time_delim"]
        parts << :minute
        headings << part_heading(tm["minute"])

        if tm["second"]
          delimiters << tm["time_delim"]
          parts << :second
          headings << part_heading(tm["second"])
        end

        if tm["frac"]
          delimiters << tm["frac_delim"]
          parts << :frac
          headings << tm["frac"].size
        end

        rest = tm["rest"]
      end

      if zm = /^#{TZ}$/.match(rest)
        delimiters << zm["zone_space"] || ''
        if zm["z"]
          # TODO ISO 8601
          parts << :zone_off
        elsif zm["zone_off"]
          parts << :zone_off
        else
          parts << :zone_abb
        end
        headings << nil

        return GuessMatch.new(delimiters, parts, headings)

      elsif rest =~ /^\s*$/
        return GuessMatch.new(delimiters, parts, headings)

      else
        return nil
      end
    end

    def part_heading(text)
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

  class RegexpMatch
    def initialize(format)
      @format
    end

    attr_reader :format

    def mergeable_group
      @format
    end
  end

  class RegexpPattern
    def initialize(regexp, format)
      @regexp = regexp
      @match = RegexpMatch.new(format)
    end

    def match(text)
      if @regexp =~ text
        return @match
      else
        return nil
      end
    end
  end

  PATTERNS = [
    GuessPattern.new,
    # TODO RFC 8222_1123
    # TODO RFC 850_1036
    # TODO APACHE_CLF
    # TODO ANSI_C
    #RegexpPattern.new(),
  ]

  def self.guess(texts)
    texts = Array(texts)
    matches = texts.map do |text|
      PATTERNS.map {|pattern| pattern.match(text) }.compact
    end.flatten
    if matches.empty?
      return nil
    elsif matches.size == 1
      return matches[0].format
    else
      match_groups = matches.group_by {|match| match.mergeable_group }
      best_match_group = match_groups.sort_by {|group| group.size }.first[1]
      best_match = best_match_group.shift
      best_match_group.each {|m| best_match.merge!(m) }
      return best_match.format
    end
  end
end
