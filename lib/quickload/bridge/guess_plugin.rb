module QuickLoad
  module Bridge
    require 'quickload/java/imports'
    require 'quickload/bridge/buffer'
    require 'quickload/bridge/data_source'

    class GuessPluginBridge
      include Java::GuessPlugin

      def guess(proc, config, sample)
        sample = Bridge::BufferBridge.to_str(sample)
        config = Bridge::DataSourceBridge.wrap(config)
        next_config = guess_buffer(config, sample)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    class LineGuessPluginBridge < Java::LineGuessPlugin
      def guessLines(proc, config, lines)
        config = Bridge::DataSourceBridge.wrap(config)
        lines = lines.to_a
        next_config = guess_lines(config, lines)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    class TextGuessPluginBridge < Java::TextGuessPlugin
      def guessText(proc, config, text)
        config = Bridge::DataSourceBridge.wrap(config)
        text = text.to_s
        next_config = guess_text(config, text)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    module GuessPluginReverseBridge
      def guess_buffer(config, sample_buffer)
        config = Bridge::DataSourceBridge.wrap(config)
        sample_buffer = Bridge::BufferBridge.to_java_buffer(sample_buffer)
        next_config = guess(config, sample_buffer)
        return Bridge::DataSourceBridge.wrap(next_config)
      end
    end
  end
end
