module Embulk::Plugin

  class Newline < TextGuess
    def guess_text(config, sample_text)
      cr_count = sample_text.count("\r")
      lf_count = sample_text.count("\n")
      crlf_count = sample_text.scan(/\r\n/).length
      if crlf_count > cr_count / 2 && crlf_count > lf_count / 2
        return {"newline" => "CRLF"}
      elsif cr_count > lf_count / 2
        return {"newline" => "CR"}
      else
        return {"newline" => "LF"}
      end
    end
  end

end
