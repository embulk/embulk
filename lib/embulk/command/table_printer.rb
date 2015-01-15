
class TablePrinter
  def initialize(column_names, options={}, &format_field_value)
    @column_names = column_names
    @samples = []
    @options = options
    @format_field_value = format_field_value
  end

  def add(record)
    if @format_field_value
      i = 0
      values = @column_names.map {|c|
        @format_field_value.call(record, c, i)
        i += 1
      }
    else
      values = record.map {|v| v.to_s }
    end

    if @samples
      # sampling mode
      @samples << values
      if @samples.size > 10
        complete_sampling!
      end
    else
      puts @format % values
    end
  end

  def complete
    complete_sampling! if @samples
    if @options[:like_partial]
      puts "..."
    else
      puts @border
    end
  end

  private

  def complete_sampling!
    border = '+-'
    format = '| '
    @column_names.each_with_index {|c,i|
      if i != 0
        format << ' | '
        border << '-+-'
      end
      col_len = max_sample_column_length(i)
      format << "%#{col_len}s"
      border << '-' * col_len
    }
    format << ' |'
    border << '-+'

    @format = format
    @border = border

    puts @border
    puts @format % @column_names
    puts @border
    @samples.each do |values|
      puts @format % values
    end
    @samples = nil
  end

  def max_sample_column_length(i)
    max_len = 0
    ([@column_names] + @samples).each {|values|
      len = values[i].size
      max_len = len if len > max_len
    }
    max_len
  end
end
