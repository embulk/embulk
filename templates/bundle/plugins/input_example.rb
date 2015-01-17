module Embulk

  class InputExample < InputPlugin
    Plugin.register_input('example', self)

    def self.transaction(config, &control)
      task = {
        'message' => config.prop('message', :string, default: nil)
      }
      threads = config.prop('threads', :int, default: 2)

      columns = [
        Column.new(0, 'col0', :long),
        Column.new(1, 'col1', :double),
        Column.new(2, 'col2', :string),
      ]

      puts "Started"
      commit_reports = yield(task, columns, threads)
      puts "Finished. Commit reports = #{reports.to_json}"

      return {}
    end

    def self.run(task, schema, index, page_builder)
      commit_report = {
      }
      return commit_report
    end
  end

end
