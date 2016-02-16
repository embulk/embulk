module Embulk
  module Guess

    class Bzip2GuessPlugin < GuessPlugin
      Plugin.register_guess('bzip2', self)

      # magic: BZ
      # version: 'h' = bzip2
      # blocksize: 1 .. 9
      # block magic: 0x314159265359 (6 bytes)
      block_magic = [0x31, 0x41, 0x59, 0x26, 0x53, 0x59].pack('C*')
      BZIP2_HEADER_PATTERN = /BZh[1-9]#{Regexp.quote(block_magic)}/n

      def guess(config, sample_buffer)
        if sample_buffer[0,10] =~ BZIP2_HEADER_PATTERN
          return {"decoders" => [{"type" => "bzip2"}]}
        end
        return {}
      end
    end

  end
end
