module Embulk

  class FilterExample < FilterPlugin
    # filter plugin file name must be: embulk/filter_<name>.rb
    Plugin.register_filter('example', self)

    def self.transaction(config, in_schema, &control)
      task = { }

      idx = in_schema.size
      out_columns = in_schema + [Column.new(idx, 'filtered', :string)]

      puts "Example filter started."
      yield(task, out_columns)
      puts "Example filter finished."
    end

    def close
    end

    def add(page)
      page.each do |record|
        @page_builder.add(record + ["added"])
      end
    end

    def finish
      @page_builder.finish
    end
  end

end
