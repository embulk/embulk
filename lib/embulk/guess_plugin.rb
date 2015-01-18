module Embulk

  class GuessPlugin
    def guess(config, sample_buffer)
      raise NotImplementedError, "GuessPlugin#guess(config, sample_buffer) must be implemented"
    end

    if Embulk.java?
      def self.java_object
        JavaAdapter.new(new)
      end

      def self.from_java_object(java_guess)
        RubyAdapter.new(java_guess)
      end

      class RubyAdapter < Embulk::GuessPlugin
        def initialized(java_guess)
          @java_guess = java_guess
        end

        def guess(config, sample)
          java_config = config.java_object
          java_sample = sample.java_object
          java_next_config = @java_guess.guess(java_config, java_sample)
          return DataSource.from_java_object(java_next_config)
        end

        def java_object
          @java_guess
        end
      end

      class JavaAdapter
        include Java::GuessPlugin

        def initialize(ruby_guess)
          @ruby_guess = ruby_guess
        end

        def guess(java_config, java_sample)
          config = DataSource.from_java_object(java_config)
          sample = Buffer.from_java_object(java_sample)
          next_config_hash = @ruby_guess.guess(config, sample)
          return DataSource.from_ruby_hash(next_config_hash).java_object
        end
      end
    end
  end

  class TextGuessPlugin < GuessPlugin
    def guess(config, sample)
      # TODO pure-ruby LineDecoder implementation?
      begin
        task = config.load_config(Java::LineDecoder::DecoderTask)
      rescue
        # TODO log?
        p $!
        p $!.backtrace
        return DataSource.new
      end

      decoder = Java::LineDecoder.new(Java::ListFileInput.new([[sample.java_object]]), task)
      sample_text = ''
      while decoder.nextFile
        first = true
        while line = decoder.poll
          if first
            first = false
          else
            sample_text << task.getNewline().getString()
          end
          sample_text << line
        end
      end

      return guess_text(config, sample_text);
    end

    def guess_text(config, sample_text)
      raise NotImplementedError, "TextGuessPlugin#guess_text(config, sample_text) must be implemented"
    end
  end

  class LineGuessPlugin < GuessPlugin
    def guess(config, sample)
      # TODO pure-ruby LineDecoder implementation?
      begin
        task = config.load_config(Java::LineDecoder::DecoderTask)
      rescue
        # TODO log?
        p $!
        p $!.backtrace
        return DataSource.new
      end

      decoder = Java::LineDecoder.new(Java::ListFileInput.new([[sample.java_object]]), task)
      sample_lines = []
      while decoder.nextFile
        while line = decoder.poll
          sample_lines << line
        end
      end

      return guess_lines(config, sample_lines);
    end

    def guess_lines(config, sample_lines)
      raise NotImplementedError, "LineGuessPlugin#guess_lines(config, sample_lines) must be implemented"
    end
  end

end
