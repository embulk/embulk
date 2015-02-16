module Embulk
  module Guess

    class GzipGuessPlugin < GuessPlugin
      Plugin.register_guess('gzip', self)

      GZIP_HEADER = "\x1f\x8b".force_encoding('ASCII-8BIT').freeze

      def guess(config, sample_buffer)
        if sample_buffer[0,2] == GZIP_HEADER
          return {"decoders" => [{"type" => "gzip"}]}
        end
        return {}
      end
    end

  end
end
