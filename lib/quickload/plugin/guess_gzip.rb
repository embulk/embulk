module QuickLoad
  class GzipGuess
    Plugin.register_guess('gzip', self)

    GZIP_HEADER = "\x1f\x8b".force_encoding('ASCII-8BIT').freeze

    def run(proc, config, sample)
      if sample[0,2] == GZIP_HEADER
        return {"file_decoders" => [{"type" => "gzip"}]}
      end
      return nil
    end
  end
end
