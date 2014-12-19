module Embulk::Plugin

  class Null < Input
    Plugin.register_input('null', self)

    def transaction(config, task)
      task['processor_count'] = config.prop('processor_count', :int, default: 0)
      task['record_count'] = config.prop('record_count', :int, default: 0)
      task.processor_count = task['processor_count']
      reports = yield
      return {}
    end

    def process(task, index, record_writer)
      task['record_count'].times do
        record_writer.add_record
      end
      return {}
    end
  end

end
