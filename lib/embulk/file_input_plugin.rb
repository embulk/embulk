module Embulk

  class FileInputPlugin
    # TODO transaction, resume, cleanup
    # TODO run

    if Embulk.java?
      # TODO to_java

      def self.from_java(java_class)
        JavaPlugin.ruby_adapter_class(java_class, FileInputPlugin, RubyAdapter)
      end

      module RubyAdapter
        module ClassMethods
          def new_java
            Java::FileInputRunner.new(Java.injector.getInstance(java_class))
          end
          # TODO transaction, resume, cleanup
        end

        # TODO run
      end
    end
  end

end
