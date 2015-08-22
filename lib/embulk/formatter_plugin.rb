module Embulk

  require 'embulk/data_source'
  require 'embulk/schema'
  require 'embulk/page'
  require 'embulk/file_output'

  class FormatterPlugin
    def self.transaction(config, schema, &control)
      yield(config)
      return {}
    end

    def initialize(task, schema, file_output)
      @task = task
      @schema = schema
      @file_output = file_output
      init
    end

    attr_reader :task, :schema, :file_output

    def init
    end

    def add(page)
      raise NotImplementedError, "FormatterPlugin#add(page) must be implemented"
    end

    def finish
    end

    def close
    end

    def self.new_java
      JavaAdapter.new(self)
    end

    class JavaAdapter
      include Java::FormatterPlugin

      def initialize(ruby_class)
        @ruby_class = ruby_class
      end

      def transaction(java_config, java_schema, java_control)
        config = DataSource.from_java(java_config)
        schema = Schema.from_java(java_schema)
        @ruby_class.transaction(config, schema) do |task_source_hash|
          java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
          java_control.run(java_task_source)
        end
        nil
      end

      def open(java_task_source, java_schema, java_file_output)
        task_source = DataSource.from_java(java_task_source)
        schema = Schema.from_java(java_schema)
        file_output = FileOutput.new(java_file_output)
        ruby_object = @ruby_class.new(task_source, schema, file_output)
        return OutputAdapter.new(ruby_object, schema, file_output)
      end

      class OutputAdapter
        include Java::TransactionalPageOutput

        def initialize(ruby_object, schema, file_output)
          @ruby_object = ruby_object
          @schema = schema
          @file_output = file_output
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
        ensure
          @file_output.close
        end
      end
    end

    def self.from_java(java_class)
      JavaPlugin.ruby_adapter_class(java_class, FormatterPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
        # TODO transaction, resume, cleanup
      end
      # TODO add, finish, close
    end
  end

end
