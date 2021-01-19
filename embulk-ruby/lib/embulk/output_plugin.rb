module Embulk

  require 'embulk/data_source'
  require 'embulk/schema'
  require 'embulk/page'

  class OutputPlugin
    def self.transaction(config, schema, task_count, &control)
      yield(config)
      return {}
    end

    def self.resume(task, schema, count, &control)
      raise NotImplementedError, "#{self}.resume(task, schema, count, &control) is not implemented. This plugin is not resumable"
    end

    def self.cleanup(task, schema, count, task_reports)
      # do nothing by default
    end

    def initialize(task, schema, index)
      @task = task
      @schema = schema
      @index = index
      init
    end

    attr_reader :task, :schema, :index

    def init
    end

    def add(page)
      raise NotImplementedError, "OutputPlugin#add(page) must be implemented"
    end

    def finish
    end

    def close
    end

    def abort
    end

    def commit
      {}
    end

    def self.new_java
      JavaAdapter.new(self)
    end

    class JavaAdapter
      include Java::OutputPlugin

      def initialize(ruby_class)
        @ruby_class = ruby_class
      end

      def transaction(java_config, java_schema, task_count, java_control)
        config = DataSource.from_java(java_config)
        schema = Schema.from_java(java_schema)
        config_diff_hash = @ruby_class.transaction(config, schema, task_count) do |task_source_hash|
          java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
          java_task_reports = java_control.run(java_task_source)
          java_task_reports.map {|java_task_report|
            DataSource.from_java(java_task_report)
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
          java_task_reports = java_control.run(java_task_source)
          java_task_reports.map {|java_task_report|
            DataSource.from_java(java_task_report)
          }
        end
        # TODO check return type of #resume
        return DataSource.from_ruby_hash(config_diff_hash).to_java
      end

      def cleanup(java_task_source, java_schema, task_count, java_task_reports)
        task_source = DataSource.from_java(java_task_source)
        schema = Schema.from_java(java_schema)
        task_reports = java_task_reports.map {|c| DataSource.from_java(c) }
        @ruby_class.cleanup(task_source, schema, task_count, task_reports)
        return nil
      end

      def open(java_task_source, java_schema, processor_index)
        task_source = DataSource.from_java(java_task_source)
        schema = Schema.from_java(java_schema)
        ruby_object = @ruby_class.new(task_source, schema, processor_index)
        return OutputAdapter.new(ruby_object, schema)
      end

      class OutputAdapter
        include Java::TransactionalPageOutput

        def initialize(ruby_object, schema)
          @ruby_object = ruby_object
          @schema = schema
        end

        def add(java_page)
          # TODO reuse page reader
          @ruby_object.add Page.new(java_page, @schema)
        end

        def finish
          @ruby_object.finish
        end

        def close
          @ruby_object.close
        end

        def abort
          @ruby_object.abort
        end

        def commit
          task_report_hash = @ruby_object.commit
          return DataSource.from_ruby_hash(task_report_hash).to_java
        end
      end
    end

    def self.from_java(java_class)
      JavaPlugin.ruby_adapter_class(java_class, OutputPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
        # TODO transaction, resume, cleanup
      end
      # TODO add, finish, close, abort, commit
    end
  end

end
