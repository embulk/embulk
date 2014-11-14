module QuickLoad::Plugin

  class CsvGuess < LineGuess
    Plugin.register_guess('csv', self)

    DELIMITER_CANDIDATES = [
      ",", "\t", "|"
    ]

    def run(config, sample_lines)
      delim = guess_delimiter(sample_lines)
      return {} unless delim

      # TODO guess quote chars
      # TODO guess header-line
      # TODO guess schema

      return {"type"=>"csv", "delimiter"=>delim}
    end

    private

    def guess_delimiter(sample_lines)
      delim_weights = DELIMITER_CANDIDATES.map do |d|
        counts = sample_lines.map {|line| line.count(d) }
        total = array_sum(counts)
        if total > 0
          stddev = array_standard_deviation(counts)
          stddev = 0.000000001 if stddev == 0.0
          weight = total / stddev
          [d, weight]
        else
          [nil, 0]
        end
      end

      delim, weight = *delim_weights.sort_by {|d,weight| weight }.last
      if delim != nil && weight > 1
        return delim
      else
        return nil
      end
    end

    def array_sum(array)
      array.inject(0) {|r,i| r += i }
    end

    def array_avg(array)
      array.inject(0.0) {|r,i| r += i } / array.size
    end

    def array_variance(array)
      avg = array_avg(array)
      array.inject(0.0) {|r,i| r += (i - avg) ** 2 } / array.size
    end

    def array_standard_deviation(array)
      Math.sqrt(array_variance(array))
    end
  end
end
