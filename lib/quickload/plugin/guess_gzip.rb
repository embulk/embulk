module QuickLoad::Plugin
  require 'quickload/plugin/guess'

  class GzipGuess < Guess
    Plugin.register_guess('gzip', self)

    GZIP_HEADER = "\x1f\x8b".force_encoding('ASCII-8BIT').freeze

    def run(config, sample_buffer)
      if sample_buffer[0,2] == GZIP_HEADER
        return {"file_decoders" => [{"type" => "gzip"}]}
      end
      return {}
    end
  end

end
