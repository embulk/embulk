module QuickLoad
  module Bridge
    require 'quickload/bridge/buffer'
    require 'quickload/bridge/data_source'

    class GuessPluginBridge
      include Java::GuessPlugin

      def guess(proc, config, sample)
        sample = Bridge::BufferBridge.to_str(sample)
        config = Bridge::DataSourceBridge.wrap(config)
        next_config = run(config, sample)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    class LineGuessPluginBridge < Java::LineGuessPlugin
      def guessLines(proc, config, lines)
        config = Bridge::DataSourceBridge.wrap(config)
        lines = lines.to_a
        next_config = run(config, lines)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    class TextGuessPluginBridge < Java::TextGuessPlugin
      def guessText(proc, config, text)
        config = Bridge::DataSourceBridge.wrap(config)
        text = text.to_s
        next_config = run(config, text)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    class GuessPluginReverseBridge
      def run(proc, config, sample)
        config = Bridge::DataSourceBridge.wrap(config)
        sample = Buffer.wrap(sample.to_java_bytes)
        next_config = guess(config, sample)
        return Bridge::DataSourceBridge.wrap(next_config)
      end
    end
  end
end
