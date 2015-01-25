module Embulk

  class InputExample < InputPlugin
    # input plugin file name must be: embulk/output_<name>.rb
    Plugin.register_input('example', self)

    def self.transaction(config, &control)
      task = {
        'message' => config.param('message', :string, default: nil)
      }
      threads = config.param('threads', :int, default: 2)

      columns = [
        Column.new(0, 'col0', :long),
        Column.new(1, 'col1', :double),
        Column.new(2, 'col2', :string),
      ]

      puts "Example input started."
      commit_reports = yield(task, columns, threads)
      puts "Example input finished. Commit reports = #{commit_reports.to_json}"

      return {}
    end

    def self.run(task, schema, index, page_builder)
      puts "Example input thread #{index}..."

      10.times do |i|
        page_builder.add([i, 10.0, "example"])
      end
      page_builder.finish  # don't forget to call finish :-)

      commit_report = {
      }
      return commit_report
    end
  end

end
