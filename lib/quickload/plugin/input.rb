module QuickLoad
  module Plugin
    require 'quickload/bridge/input_plugin'

    class Input < Bridge::InputPluginBridge
      def transaction(config, task)
        raise NotImplementedError, "Input#transaction(config, task, &block) must be implemented"
      end

      def process(task, index, record_writer)
        raise NotImplementedError, "Input#process(task, index, record_writer) must be implemented"
      end
    end

    class FileInput < Bridge::FileInputPluginBridge
      def transaction(config, task)
        raise NotImplementedError, "FileInput#transaction(config, task, &block) must be implemented"
      end

      def process_files(task, index, file_writer)
        raise NotImplementedError, "FileInput#process(task, index, file_writer) must be implemented"
      end
    end

  end
end
