module QuickLoad
  module Plugin
    require 'quickload/bridge/guess_plugin'

    class Guess < Bridge::GuessPluginBridge
      def guess_buffer(config, sample_buffer)
        raise NotImplementedError, "Guess#guess_buffer(config, sample_buffer) must be implemented"
      end
    end

    class LineGuess < Bridge::LineGuessPluginBridge
      def guess_lines(config, sample_lines)
        raise NotImplementedError, "LineGuess#guess_lines(config, sample_lines) must be implemented"
      end
    end

    class TextGuess < Bridge::TextGuessPluginBridge
      def guess_text(config, sample_text)
        raise NotImplementedError, "TextGuess#guess_text(config, sample_text) must be implemented"
      end
    end

  end
end
