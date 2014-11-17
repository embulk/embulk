module QuickLoad
  module Bridge
    require 'quickload/java/imports'
    require 'quickload/bridge/data_source'
    require 'quickload/bridge/schema'
    require 'quickload/task_config'
    require 'quickload/record_writer'
    require 'quickload/file_writer'

    class InputPluginBridge
      include Java::InputPlugin

      def runInputTransaction(proc, config, control)
        config = DataSourceBridge.wrap(config)
        task = TaskConfig.new(proc)
        next_config = transaction(config, task) do
          proc.setSchema(Bridge::Schema.new(task.columns).to_java)
          proc.setProcessorCount(task.processor_count)
          reports = control.run(DataSourceBridge.to_java_task_source(task))
          reports.to_a.map {|report| DataSourceBridge.wrap(report) }
        end
        DataSourceBridge.to_java_next_config(next_config)
      end

      def runInput(proc, taskSource, processorIndex, pageOutput)
        task = TaskConfig.new(proc, DataSourceBridge.wrap(taskSource))
        record_writer = RecordWriter.new(task, pageOutput)
        report = process(task, processorIndex, record_writer)
        DataSourceBridge.to_java_report(report)
      end
    end

    class FileInputPluginBridge < Java::FileInputPlugin
      def runFileInputTransaction(proc, config, control)
        config = DataSourceBridge.wrap(config)
        task = TaskConfig.new(proc)
        next_config = transaction(config, task) do
          proc.setProcessorCount(task.processor_count)
          reports = control.run(DataSourceBridge.to_java_task_source(task))
          reports.to_a.map {|report| DataSourceBridge.wrap(report) }
        end
        DataSourceBridge.to_java_next_config(next_config)
      end

      def runFileInput(proc, taskSource, processorIndex, fileBufferOutput)
        task = TaskConfig.new(proc, DataSourceBridge.wrap(taskSource))
        file_writer = FileWriter.new(fileBufferOutput)
        report = process_files(task, processorIndex, file_writer)
        DataSourceBridge.to_java_report(report)
      end
    end

  end
end
