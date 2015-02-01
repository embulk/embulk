module Embulk

  class FilterPlugin
    def self.transaction(config, in_schema, &control)
      yield(config)
      return {}
    end

    def initialize(task, in_schema, out_schema, page_builder)
      @task = task
      @in_schema = in_schema
      @out_schema = out_schema
      @page_builder = page_builder
    end

    attr_reader :task, :in_schema, :out_schema, :page_builder

    def add(page)
      raise NotImplementedError, "FilterPlugin#add(page) must be implemented"
    end

    def finish
    end

    def close
    end

    if Embulk.java?
      def self.java_object
        JavaAdapter.new(self)
      end

      class JavaAdapter
        include Java::FilterPlugin

        def initialize(ruby_class)
          @ruby_class = ruby_class
        end

        def transaction(java_config, java_in_schema, java_control)
          config = DataSource.from_java_object(java_config)
          in_schema = Schema.from_java_object(java_in_schema)
          @ruby_class.transaction(config, in_schema) do |task_source_hash, out_columns|
            java_task_source = DataSource.from_ruby_hash(task_source_hash).java_object
            java_out_schemas = Schema.new(out_columns).java_object
            java_control.run(java_task_source, java_out_schemas)
          end
          nil
        end

        def open(java_task_source, java_in_schema, java_out_schema, java_output)
          task_source = DataSource.from_java_object(java_task_source)
          in_schema = Schema.from_java_object(java_in_schema)
          out_schema = Schema.from_java_object(java_out_schema)
          page_builder = PageBuilder.new(out_schema, java_output)
          ruby_object = @ruby_class.new(task_source, in_schema, out_schema, page_builder)
          return OutputAdapter.new(ruby_object, in_schema, page_builder)
        end

        class OutputAdapter
          include Java::TransactionalPageOutput

          def initialize(ruby_object, in_schema, page_builder)
            @ruby_object = ruby_object
            @in_schema = in_schema
            @page_builder = page_builder
          end

          def add(java_page)
            @ruby_object.add Page.new(java_page, @in_schema)
          end

          def finish
            @ruby_object.finish
          end

          def close
            @ruby_object.close
          ensure
              @page_builder.close
          end
        end
      end
    end
  end
end
