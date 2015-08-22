module Embulk

  require 'embulk/data_source'
  require 'embulk/schema'
  require 'embulk/page_builder'
  require 'embulk/file_input'

  class ParserPlugin
    def self.transaction(config, &control)
      raise NotImplementedError, "ParserPlugin.transaction(config, &control) must be implemented"
    end

    def initialize(task, schema, page_builder)
      @task = task
      @schema = schema
      @page_builder = page_builder
      init
    end

    attr_reader :task, :schema, :page_builder

    def init
    end

    def run(file_input)
      raise NotImplementedError, "ParserPlugin#run(file_input) must be implemented"
    end

    def self.new_java
      JavaAdapter.new(self)
    end

    class JavaAdapter
      include Java::ParserPlugin

      def initialize(ruby_class)
        @ruby_class = ruby_class
      end

      def transaction(java_config, java_control)
        config = DataSource.from_java(java_config)
        @ruby_class.transaction(config) do |task_source_hash,columns|
          java_task_source = DataSource.from_ruby_hash(task_source_hash).to_java
          java_schema = Schema.new(columns).to_java
          java_control.run(java_task_source, java_schema)
        end
        nil
      end

      def run(java_task_source, java_schema, java_file_input, java_output)
        task_source = DataSource.from_java(java_task_source)
        schema = Schema.from_java(java_schema)
        file_input = FileInput.new(java_file_input)
        page_builder = PageBuilder.new(schema, java_output)
        begin
          @ruby_class.new(task_source, schema, page_builder).run(file_input)
          nil
        ensure
          page_builder.close
          # FileInput is closed by FileInputRunner
        end
      end
    end

    def self.from_java(java_class)
      JavaPlugin.ruby_adapter_class(java_class, ParserPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
      end
      # TODO
    end
  end

end
