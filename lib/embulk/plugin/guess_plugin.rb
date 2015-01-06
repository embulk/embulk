module Embulk
  module Plugin

    class GuessPlugin
      def guess_buffer(config, sample_buffer)
        raise NotImplementedError, "GuessPlugin#guess_buffer(config, sample_buffer) must be implemented"
      end

      if Embulk.java?
        class JavaAdapter < Embulk::Plugin::GuessPlugin
          def guess_buffer(config, sample)
            guess(config, sample)
          end
        end

        def self.ruby_object(java)
          JavaAdapter.new(java)
        end

        include Java::GuessPlugin

        def java_object
          self
        end

        def guess(config, sample)
          hash = guess_buffer(config, sample)
          hash = DataSource.new.merge!(hash) unless hash.is_a?(DataSource)
          hash.java_object
        end
      end
    end

    class TextGuessPlugin < GuessPlugin
      def guess_buffer(config, sample_buffer)
        # TODO pure-ruby LineDecoder implementation?
        begin
          task = config.load_config(Java::LineDecoder::DecoderTask)
        rescue
          # TODO log?
          p $!
          p $!.backtrace
          return DataSource.new
        end

        decoder = Java::LineDecoder.new(Java::ListFileInput.new([sample_buffer.java_object]), task)
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
      def guess_buffer(config, sample_buffer)
        # TODO pure-ruby LineDecoder implementation?
        begin
          task = config.load_config(Java::LineDecoder::DecoderTask)
        rescue
          # TODO log?
          p $!
          p $!.backtrace
          return DataSource.new
        end

        decoder = Java::LineDecoder.new(Java::ListFileInput.new([sample_buffer.java_object]), task)
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
end
