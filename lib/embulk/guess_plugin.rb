module Embulk

  class GuessPlugin
    def guess(config, sample_buffer)
      raise NotImplementedError, "GuessPlugin#guess(config, sample_buffer) must be implemented"
    end

    if Embulk.java?
      def self.new_java
        JavaAdapter.new(new)
      end

      class JavaAdapter
        include Java::GuessPlugin

        def initialize(ruby_guess)
          @ruby_guess = ruby_guess
        end

        def guess(java_config, java_sample)
          config = DataSource.from_java(java_config)
          sample = Buffer.from_java(java_sample)
          config_diff_hash = @ruby_guess.guess(config, sample)
          return DataSource.from_ruby_hash(config_diff_hash).to_java
        end
      end

      def self.from_java(java_class)
        JavaPlugin.ruby_adapter(java_class, GuessPlugin, RubyAdapter)
      end

      module RubyAdapter
        module ClassMethods
        end

        def guess(config, sample)
          java_config = config.to_java
          java_sample = sample.to_java
          java_config_diff = java_object.guess(java_config, java_sample)
          return DataSource.from_java(java_config_diff)
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

      decoder = Java::LineDecoder.new(Java::ListFileInput.new([[sample.to_java]]), task)
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

      decoder = Java::LineDecoder.new(Java::ListFileInput.new([[sample.to_java]]), task)
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
