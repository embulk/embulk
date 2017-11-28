module Embulk
  module Input

    class ExampleInputPlugin < InputPlugin
      # input plugin file name must be: embulk/input/<name>.rb
      Plugin.register_input('example', self)

      def self.transaction(config, &control)
        files = ['file1', 'file2']
        task = {
          'files' => files,
          'hostname' => config.param('hostname', :string, default: nil)
        }

        columns = [
          Column.new(0, 'file', :string),
          Column.new(1, 'hostname', :string),
          Column.new(2, 'col0', :long),
          Column.new(3, 'col1', :double),
        ]

        resume(task, columns, files.length, &control)
      end

      def self.resume(task, columns, count, &control)
        puts "Example input started."
        task_reports = yield(task, columns, count)
        puts "Example input finished. Commit reports = #{task_reports.to_json}"

        next_config_diff = {}
        return next_config_diff
      end

      def initialize(task, schema, index, page_builder)
        super
        @file = task['files'][index]
        @hostname = task['hostname']
      end

      def run
        puts "Example input thread #{@index}..."

        10.times do |i|
          @page_builder.add([@file, @hostname, i, 10.0])
        end
        @page_builder.finish  # don't forget to call finish :-)

        task_report = {}
        return task_report
      end
    end

  end
end
