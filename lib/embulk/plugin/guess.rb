module Embulk
  module Plugin

    class Guess
      def guess_buffer(config, sample_buffer)
        raise NotImplementedError, "Guess#guess_buffer(config, sample_buffer) must be implemented"
      end
    end

    class LineGuess
      def guess_lines(config, sample_lines)
        raise NotImplementedError, "LineGuess#guess_lines(config, sample_lines) must be implemented"
      end
    end

    class TextGuess
      def guess_text(config, sample_text)
        raise NotImplementedError, "TextGuess#guess_text(config, sample_text) must be implemented"
      end
    end

    require 'embulk/bridge/plugin'
    require 'embulk/bridge/guess_plugin'
    Embulk::Bridge::Plugin.initialize_bridge('guess', Guess, Embulk::Bridge::GuessPluginBridge)
    Embulk::Bridge::Plugin.initialize_bridge('guess', LineGuess, Embulk::Bridge::LineGuessPluginBridge)
    Embulk::Bridge::Plugin.initialize_bridge('guess', TextGuess, Embulk::Bridge::TextGuessPluginBridge)
  end
end
