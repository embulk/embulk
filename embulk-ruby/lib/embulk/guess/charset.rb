module Embulk
  module Guess

    class CharsetGuessPlugin < GuessPlugin
      Plugin.register_guess('charset', self)

      def guess(config, sample_buffer)
        Embulk.logger.warn(
            "Ruby-based CharsetGuess is no longer available. "
            + "It always returns \"UTF-8\" unconditionally. Use an appropriate guess plugin explicitly."
        return {"parser" => {"charset" => "UTF-8"}}
      end
    end

  end
end
