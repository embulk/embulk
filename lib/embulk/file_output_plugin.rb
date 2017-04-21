module Embulk

  require 'embulk/data_source'
  require 'embulk/buffer'

  class FileOutputPlugin
    def self.transaction(config, task_count, &control)
      yield(config)
      return {}
    end

    def self.resume(task, count, &control)
      raise NotImplementedError, "#{self}.resume(task, count, &control) is not implemented. This plugin is not resumable"
    end

    def self.cleanup(task, count, task_reports)
      # do nothing by default
    end

    def initialize(task, file_output, index)
      @task = task
      @file_output = file_output
      @index = index
      init
    end

    attr_reader :task, :file_output, :index

    def init
    end

    def add(buffer)
      raise NotImplementedError, "FileOutputPlugin#add(buffer) must be implemented"
    end

    def finish
    end

    def close
    end

    def next_file
    end

    def abort
    end

    def commit
      {}
    end

    # TODO to_java
    def self.new_java
      JavaAdapter.new(self)
    end

    class JavaAdapter
      include Java::FileOutputPlugin

      def initialize(ruby_class)
        @ruby_class = ruby_class
      end

      def transaction(java_config, task_count, java_control)
        config = DataSource.from_java(java_config)
        config_diff_hash = @ruby_class.transaction(config, task_count) do |task_source_hash|
          java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
          java_task_reports = java_control.run(java_task_source)
          java_task_reports.map {|java_task_report|
            DataSource.from_java(java_task_report)
          }
        end
        # TODO check return type of #transaction
        return DataSource.from_ruby_hash(config_diff_hash).to_java
      end

      def resume(java_task_source, task_count, java_control)
        task_source = DataSource.from_java(java_task_source)
        config_diff_hash = @ruby_class.resume(task_source, task_count) do |task_source_hash,task_count|
          java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
          java_task_reports = java_control.run(java_task_source)
          java_task_reports.map {|java_task_report|
            DataSource.from_java(java_task_report)
          }
        end
        # TODO check return type of #resume
        return DataSource.from_ruby_hash(config_diff_hash).to_java
      end

      def cleanup(java_task_source, task_count, java_task_reports)
        task_source = DataSource.from_java(java_task_source)
        task_reports = java_task_reports.map {|c| DataSource.from_java(c) }
        @ruby_class.cleanup(task_source, task_count, task_reports)
        return nil
      end

      def open(java_task_source, processor_index, java_file_output)
        task_source = DataSource.from_java(java_task_source)
        file_output = FileOutput.new(java_file_output)
        ruby_object = @ruby_class.new(task_source, file_output, processor_index)
        return OutputAdapter.new(ruby_object, file_output)
      end

      class OutputAdapter
        include Java::TransactionalFileOutput

        def initialize(ruby_object, file_output)
          @ruby_object = ruby_object
          @file_output = file_output
        end

        def next_file
          @ruby_object.next_file
          self
        end

        def add(java_buffer)
          @ruby_object.add(java_buffer)
        end

        def finish
          @ruby_object.finish
        end

        def close
          @ruby_object.close
        ensure
          @file_output.close
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
      JavaPlugin.ruby_adapter_class(java_class, FileOutputPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
        def new_java
          Java::FileOutputRunner.new(Java.injector.getInstance(plugin_java_class))
        end
        # TODO transaction, resume, cleanup
      end

      # TODO add, finish, close, abort, commit
    end
  end

end
