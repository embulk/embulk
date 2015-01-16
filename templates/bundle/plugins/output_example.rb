module Embulk

  class OutputExample
    Plugin.register_output('example', self)

    def self.transaction(config, schema, processor_count, &control)
      task = {
        'message' => config.prop('message', :string, default: nil)
      }

      puts "Started"
      reports = yield(task)
      puts "Finished. Commit reports = #{reports.to_json}"

      return {}
    end

    def initialize(task, schema, index)
      super
      @records = 0
    end

    def close
    end

    def add(page)
      page.each do |record|
        hash = schema.zip(record)
        puts "#{message}: #{hash.to_json}"
        @records += 1
      end
    end

    def finish
    end

    def abort
    end

    def commit
      {
        "records" => @records
      }
    end
  end

end
