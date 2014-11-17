module QuickLoad
  module Plugin
    require 'quickload/bridge/parser_plugin'

    class Parser < Bridge::ParserPluginBridge
      def configure(config, task)
        raise NotImplementedError, "Parser#configure(config, task) must be implemented"
      end

      def process(task, index, file_reader, record_writer)
        raise NotImplementedError, "Parser#process(task, index, file_reader, record_writer) must be implemented"
      end
    end

    # TODO
    #class TextParser

    # TODO
    #class LineParser

  end
end
