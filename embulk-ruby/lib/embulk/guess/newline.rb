module Embulk
  module Guess

    class NewlineGuessPlugin < TextGuessPlugin
      Plugin.register_guess('newline', self)

      def guess(config, sample)
        if config.fetch('parser', {}).fetch('charset', nil).nil?
          require 'embulk/guess/charset'
          charset_guess = Guess::CharsetGuessPlugin.new
          return charset_guess.guess(config, sample)
        end

        cr_count = sample.count("\r")
        lf_count = sample.count("\n")
        crlf_count = sample.scan(/\r\n/).length
        if crlf_count > cr_count / 2 && crlf_count > lf_count / 2
          return {"parser" => {"newline" => "CRLF"}}
        elsif cr_count > lf_count / 2
          return {"parser" => {"newline" => "CR"}}
        else
          return {"parser" => {"newline" => "LF"}}
        end
      end
    end

  end
end
