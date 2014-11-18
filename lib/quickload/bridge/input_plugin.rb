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

      def runInputTransaction(exec, config, control)
        config = DataSourceBridge.wrap(config)
        task = TaskConfig.new(exec)
        next_config = transaction(config, task) do
          exec.setSchema(Bridge::Schema.new(task.columns).to_java)
          exec.setProcessorCount(task.processor_count)
          reports = control.run(DataSourceBridge.to_java_task_source(task))
          reports.to_a.map {|report| DataSourceBridge.wrap(report) }
        end
        DataSourceBridge.to_java_next_config(next_config)
      end

      def runInput(exec, taskSource, processorIndex, pageOutput)
        task = TaskConfig.new(exec, DataSourceBridge.wrap(taskSource))
        record_writer = RecordWriter.new(task, pageOutput)
        report = process(task, processorIndex, record_writer)
        DataSourceBridge.to_java_report(report)
      end
    end

    class FileInputPluginBridge < Java::FileInputPlugin
      def runFileInputTransaction(exec, config, control)
        config = DataSourceBridge.wrap(config)
        task = TaskConfig.new(exec)
        next_config = transaction(config, task) do
          exec.setProcessorCount(task.processor_count)
          reports = control.run(DataSourceBridge.to_java_task_source(task))
          reports.to_a.map {|report| DataSourceBridge.wrap(report) }
        end
        DataSourceBridge.to_java_next_config(next_config)
      end

      def runFileInput(exec, taskSource, processorIndex, fileBufferOutput)
        task = TaskConfig.new(exec, DataSourceBridge.wrap(taskSource))
        file_writer = FileWriter.new(fileBufferOutput)
        report = process_files(task, processorIndex, file_writer)
        DataSourceBridge.to_java_report(report)
      end
    end

  end
end
