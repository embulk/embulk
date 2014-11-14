module QuickLoad
  module Plugin

    class Guess < Bridge::GuessPluginBridge
      def run(config, sample_buffer)
        raise "Guess#run(config, sample_buffer) is not implemented"
      end
    end

    class LineGuess < Bridge::LineGuessPluginBridge
      def run(config, sample_lines)
        raise "LineGuess#run(config, sample_lines) is not implemented"
      end
    end

    class TextGuess < Bridge::TextGuessPluginBridge
      def run(config, sample_text)
        raise "TextGuess#run(config, sample_text) is not implemented"
      end
    end

  end
end
