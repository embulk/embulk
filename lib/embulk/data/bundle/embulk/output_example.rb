module Embulk

  class OutputExample < OutputPlugin
    # output plugin file name must be: embulk/output_<name>.rb
    Plugin.register_output('example', self)

    def self.transaction(config, schema, processor_count, &control)
      task = {
        'message' => config.param('message', :string, default: "record")
      }

      puts "Example output started."
      commit_reports = yield(task)
      puts "Example output finished. Commit reports = #{commit_reports.to_json}"

      return {}
    end

    def initialize(task, schema, index)
      puts "Example output thread #{index}..."
      super
      @message = task.param('message', :string)
      @records = 0
    end

    def close
    end

    def add(page)
      page.each do |record|
        hash = Hash[schema.names.zip(record)]
        puts "#{@message}: #{hash.to_json}"
        @records += 1
      end
    end

    def finish
    end

    def abort
    end

    def commit
      commit_report = {
        "records" => @records
      }
      return commit_report
    end
  end

end
