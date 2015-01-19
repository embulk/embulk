module Embulk

  require 'embulk/data_source'
  require 'embulk/schema'
  require 'embulk/page'

  class OutputPlugin
    def self.transaction(config, schema, processor_count, &control)
      yield(config)
      return {}
    end

    def initialize(task, schema, index)
      @task = task
      @schema = schema
      @index = index
    end

    attr_reader :task, :schema, :index

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

    if Embulk.java?
      def self.java_object
        JavaAdapter.new(self)
      end

      class JavaAdapter
        include Java::OutputPlugin

        def initialize(ruby_class)
          @ruby_class = ruby_class
        end

        def transaction(java_config, java_schema, processor_count, java_control)
          config = DataSource.from_java_object(java_config)
          schema = Schema.from_java_object(java_schema)
          next_config_hash = @ruby_class.transaction(config, schema, processor_count) do |task_source_hash|
            java_task_source = DataSource.from_ruby_hash(task_source_hash).java_object
            java_commit_reports = java_control.run(java_task_source)
            java_commit_reports.map {|java_commit_report|
              DataSource.from_java_object(java_commit_report)
            }
          end
          # TODO check return type of #transaction
          return DataSource.from_ruby_hash(next_config_hash).java_object
        end

        def open(java_task_source, java_schema, processor_index)
          task_source = DataSource.from_java_object(java_task_source)
          schema = Schema.from_java_object(java_schema)
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
            commit_report_hash = @ruby_object.commit
            return DataSource.from_ruby_hash(commit_report_hash).java_object
          end
        end
      end
    end
  end

end
