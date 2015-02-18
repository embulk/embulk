module Embulk

  require 'embulk/data_source'
  require 'embulk/schema'
  require 'embulk/page_builder'

  class InputPlugin
    def self.transaction(config, &control)
      raise NotImplementedError, "InputPlugin.transaction(config, &control) must be implemented"
    end

    def self.resume(task, columns, count, &control)
      raise NotImplementedError, "#{self}.resume(task, columns, count, &control) is not implemented. This plugin is not resumable"
    end

    def self.cleanup(task, schema, count, commit_reports)
      # do nothing by default
    end

    def initialize(task, schema, index, page_builder)
      @task = task
      @schema = schema
      @index = index
      @page_builder = page_builder
      init
    end

    def init
    end

    def run
      raise NotImplementedError, "InputPlugin#run must be implemented"
    end

    if Embulk.java?
      def self.new_java
        JavaAdapter.new(self)
      end

      class JavaAdapter
        include Java::InputPlugin

        def initialize(ruby_class)
          @ruby_class = ruby_class
        end

        def transaction(java_config, java_control)
          config = DataSource.from_java(java_config)
          config_diff_hash = @ruby_class.transaction(config) do |task_source_hash,columns,task_count|
            java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
            java_schema = Schema.new(columns).to_java
            java_commit_reports = java_control.run(java_task_source, java_schema, task_count)
            java_commit_reports.map {|java_commit_report|
              DataSource.from_java(java_commit_report)
            }
          end
          # TODO check return type of #transaction
          return DataSource.from_ruby_hash(config_diff_hash).to_java
        end

        def resume(java_task_source, java_schema, task_count, java_control)
          task_source = DataSource.from_java(java_task_source)
          schema = Schema.from_java(java_schema)
          config_diff_hash = @ruby_class.resume(task_source, schema, task_count) do |task_source_hash,columns,task_count|
            java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
            java_schema = Schema.new(columns).to_java
            java_commit_reports = java_control.run(java_task_source, java_schema, task_count)
            java_commit_reports.map {|java_commit_report|
              DataSource.from_java(java_commit_report)
            }
          end
          # TODO check return type of #resume
          return DataSource.from_ruby_hash(config_diff_hash).to_java
        end

        def cleanup(java_task_source, java_schema, task_count, java_commit_reports)
          task_source = DataSource.from_java(java_task_source)
          schema = Schema.from_java(java_schema)
          commit_reports = java_commit_reports.map {|c| DataSource.from_java(c) }
          @ruby_class.cleanup(task_source, schema, task_count, commit_reports)
          return nil
        end

        def run(java_task_source, java_schema, processor_index, java_output)
          task_source = DataSource.from_java(java_task_source)
          schema = Schema.from_java(java_schema)
          page_builder = PageBuilder.new(schema, java_output)
          begin
            commit_report_hash = @ruby_class.new(task_source, schema, processor_index, page_builder).run
            return DataSource.from_ruby_hash(commit_report_hash).to_java
          ensure
            page_builder.close
          end
        end
      end

      def self.from_java(java_class)
        JavaPlugin.ruby_adapter_class(java_class, InputPlugin, RubyAdapter)
      end

      module RubyAdapter
        module ClassMethods
        end
        # TODO
      end
    end
  end

end
