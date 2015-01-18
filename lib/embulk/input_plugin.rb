module Embulk

  require 'embulk/data_source'
  require 'embulk/column'
  require 'embulk/page_builder'

  class InputPlugin
    def self.transaction(config, &control)
      raise NotImplementedError, "InputPlugin.transaction(config, &control) must be implemented"
    end

    def self.run(task, schema, index, page_builder)
      raise NotImplementedError, "InputPlugin#run(task, schema, index, page_builder) must be implemented"
    end

    if Embulk.java?
      def self.java_object
        JavaAdapter.new(self)
      end

      class JavaAdapter
        include Java::InputPlugin

        def initialize(ruby_class)
          @ruby_class = ruby_class
        end

        def transaction(java_config, java_control)
          config = DataSource.from_java_object(java_config)
          next_config_hash = @ruby_class.transaction(config) do |task_source_hash,columns,processor_count|
            java_task_source = DataSource.from_ruby_hash(task_source_hash).java_object
            java_schema = Schema.new(columns).java_object
            java_commit_reports = java_control.run(java_task_source, java_schema, processor_count)
            java_commit_reports.map {|java_commit_report|
              DataSource.from_java_object(java_commit_report)
            }
          end
          # TODO check return type of #transaction
          return DataSource.from_ruby_hash(next_config_hash).java_object
        end

        def run(java_task_source, java_schema, processor_index, java_output)
          task_source = DataSource.from_java_object(java_task_source)
          schema = Schema.from_java_object(java_schema)
          page_builder = PageBuilder.new(schema, java_output)
          commit_report_hash = @ruby_class.run(task_source, schema, processor_index, page_builder)
          return DataSource.from_ruby_hash(commit_report_hash).java_object
        end
      end
    end
  end

end
