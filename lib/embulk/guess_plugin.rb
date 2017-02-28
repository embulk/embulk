module Embulk

  require 'embulk/guess/schema_guess'

  class GuessPlugin
    def guess(config, sample_buffer)
      raise NotImplementedError, "GuessPlugin#guess(config, sample_buffer) must be implemented"
    end

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
      JavaPlugin.ruby_adapter_class(java_class, GuessPlugin, RubyAdapter)
    end

    module RubyAdapter
      module ClassMethods
      end

      def guess(config, sample)
        java_config = config.to_java
        java_sample = sample.to_java
        java_config_diff = plugin_java_object.guess(java_config, java_sample)
        return DataSource.from_java(java_config_diff)
      end
    end
  end

  class TextGuessPlugin < GuessPlugin
    def guess(config, sample)
      if config.fetch('parser', {}).fetch('charset', nil).nil?
        require 'embulk/guess/charset'
        charset_guess = Guess::CharsetGuessPlugin.new
        return charset_guess.guess(config, sample)
      end

      # TODO pure-ruby LineDecoder implementation?
      begin
        parser_task = config.param("parser", :hash, default: {}).load_config(Java::LineDecoder::DecoderTask)
      rescue
        # TODO log?
        p $!
        p $!.backtrace
        return DataSource.new
      end

      decoder = Java::LineDecoder.new(Java::ListFileInput.new([[sample.to_java]]), parser_task)
      sample_text = ''
      while decoder.nextFile
        first = true
        while line = decoder.poll
          if first
            first = false
          else
            sample_text << parser_task.getNewline().getString()
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
      if config.fetch('parser', {}).fetch('charset', nil).nil?
        require 'embulk/guess/charset'
        charset_guess = Guess::CharsetGuessPlugin.new
        return charset_guess.guess(config, sample)
      end

      if config.fetch('parser', {}).fetch('newline', nil).nil?
        require 'embulk/guess/newline'
        newline_guess = Guess::NewlineGuessPlugin.new
        return newline_guess.guess(config, sample)
      end

      # TODO pure-ruby LineDecoder implementation?
      begin
        parser_task = config.param("parser", :hash, default: {}).load_config(Java::LineDecoder::DecoderTask)
      rescue
        # TODO log?
        p $!
        p $!.backtrace
        return DataSource.new
      end

      decoder = Java::LineDecoder.new(Java::ListFileInput.new([[sample.to_java]]), parser_task)
      sample_lines = []
      while decoder.nextFile
        while line = decoder.poll
          sample_lines << line
        end
        unless sample.end_with?(parser_task.getNewline.getString)
          sample_lines.pop unless sample_lines.empty? # last line is partial
        end
      end

      return guess_lines(config, sample_lines);
    end

    def guess_lines(config, sample_lines)
      raise NotImplementedError, "LineGuessPlugin#guess_lines(config, sample_lines) must be implemented"
    end
  end

end
