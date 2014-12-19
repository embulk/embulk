module Embulk::Plugin

  class Gzip < Guess
    GZIP_HEADER = "\x1f\x8b".force_encoding('ASCII-8BIT').freeze

    def guess_buffer(config, sample_buffer)
      if sample_buffer[0,2] == GZIP_HEADER
        return {"file_decoders" => [{"type" => "gzip"}]}
      end
      return {}
    end
  end

end
