module Embulk
  module Guess

    class CharsetGuessPlugin < GuessPlugin
      Plugin.register_guess('charset', self)

      STATIC_MAPPING = {
        # ISO-8859-1 means ASCII which is a subset of UTF-8 in most of cases
        # due to lack of sample data set.
        "ISO-8859-1" => "UTF-8",

        # Shift_JIS is used almost only by Windows that uses "CP932" in fact.
        # And "CP932" called by Microsoft actually means "MS932" in Java.
        "Shift_JIS" => "MS932",
      }

      def guess(config, sample_buffer)
        # ICU4J
        begin
          detector_class = com.ibm.icu.text.CharsetDetector
        rescue NameError
          # icu4j is removed from embulk.gem package explicitly at embulk.gemspec
          # if gem is packaged for JRuby to reduce binary size. Instead, if it's
          # packaged for JRuby, embulk.gemspec adds rjack-icu to its dependency.
          require 'rjack-icu'
          detector_class = com.ibm.icu.text.CharsetDetector
        end
        detector = detector_class.new
        detector.setText(sample_buffer.to_java_bytes)
        best_match = detector.detect
        if best_match.getConfidence < 50
          name = "UTF-8"
        else
          name = best_match.getName
          if mapped_name = STATIC_MAPPING[name]
            name = mapped_name
          end
        end
        return {"parser" => {"charset" => name}}
      end
    end

  end
end
