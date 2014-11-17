module QuickLoad::Plugin

  class CsvGuess < LineGuess
    Plugin.register_guess('csv', self)

    DELIMITER_CANDIDATES = [
      ",", "\t", "|"
    ]

    QUOTE_CANDIDATES = [
      "\"", "'"
    ]

    def guess_lines(config, sample_lines)
      delim = guess_delimiter(sample_lines)
      unless delim
        # not CSV file
        return {}
      end

      guessed = {"type"=>"csv", "delimiter"=>delim}

      # TODO guess quote chars

      sample_records = sample_lines.map {|line| line.split(delim) }
      first_types = guess_field_types(sample_records[0, 1])
      other_types = guess_field_types(sample_records[1..-1])

      if first_types.size <= 1 || other_types.size <= 1
        # guess failed
        return {}
      end

      unless config.has_key?("header_line")
        guessed["header_line"] = (first_types != other_types && !first_types.any? {|t| t != "string" })
      end

      unless config.has_key?("columns")
        if guessed["header_line"] || config["header_line"]
          column_names = sample_records.first
        else
          column_names = (0..other_types.size).to_a.map {|i| "c#{i}" }
        end
        schema = []
        column_names.zip(other_types).each do |name,type|
          if name && type
            schema << {"name" => name, "type" => type}
          end
        end
        guessed["columns"] = schema
      end

      return guessed
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

    def guess_field_types(field_lines)
      column_lines = []
      field_lines.each do |fields|
        fields.each_with_index {|field,i| (column_lines[i] ||= []) << guess_type(field) }
      end
      columns = column_lines.map do |types|
        types.inject(nil) {|r,t| merge_type(r,t) }
      end
      return columns
    end

    def merge_type(type1, type2)
      case type1
      when nil
        return type2

      when "string"
        return "string"

      when "long"
        if type2 == "double"
          return "double"
        elsif type2 == "long"
          return "long"
        else
          return "string"
        end

      when "double"
        if ["long", "double"].include?(type2)
          return "double"
        else
          return "string"
        end
      end
    end

    def guess_type(str)
      if str.to_i.to_s == str
        return "long"
      end

      if str.include?('.')
        a, b = str.split(".", 2)
        if a.to_i.to_s == a && b.to_i.to_s == b
          return "double"
        end
      end

      return "string"
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
