module Embulk
  module Filter

    class ExampleFilterPlugin < FilterPlugin
      # filter plugin file name must be: embulk/filter/<name>.rb
      Plugin.register_filter('example', self)

      def self.transaction(config, in_schema, &control)
        task = {
          'key' => config.param('key', :string, default: "filter_key"),
          'value' => config.param('value', :string, default: "filter_value")
        }

        idx = in_schema.size
        out_columns = in_schema + [Column.new(idx, task['key'], :string)]

        puts "Example filter started."
        yield(task, out_columns)
        puts "Example filter finished."
      end

      def initialize(task, in_schema, out_schema, page_builder)
        super
        @value = task['value']
      end

      def close
      end

      def add(page)
        page.each do |record|
          @page_builder.add(record + [@value])
        end
      end

      def finish
        @page_builder.finish
      end
    end

  end
end
