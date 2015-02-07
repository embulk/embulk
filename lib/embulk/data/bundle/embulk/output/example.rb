module Embulk
  module Output

    class Example < OutputPlugin
      # output plugin file name must be: embulk/output_<name>.rb
      Plugin.register_output('example', self)

      def self.transaction(config, schema, count, &control)
        task = {
          'message' => config.param('message', :string, default: "record")
        }

        resume(task, schema, count, &control)
      end

      def self.resume(task, schema, count, &control)
        puts "Example output started."
        commit_reports = yield(task)
        puts "Example output finished. Commit reports = #{commit_reports.to_json}"

        next_config_diff = {}
        return next_config_diff
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
          STDOUT.write "#{@message}: #{hash.to_json}\n"
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
end
