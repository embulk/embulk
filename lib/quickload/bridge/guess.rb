module QuickLoad
  module Bridge
    require 'quickload/java_imports'
    require 'quickload/bridge/buffer'
    require 'quickload/bridge/data_source'

    class GuessPluginBridge
      java_implements ParserGuessPlugin

      def initialize(rb)
        @rb = rb
      end

      def guess(proc, config, sample)
        sample = Bridge::BufferBridge.to_str(sample)
        config = Bridge::DataSourceBridge.wrap(config)
        next_config = @rb.run(proc, config, sample)
        return Bridge::DataSourceBridge.to_java_next_config(next_config)
      end
    end

    class GuessPluginReverseBridge
      def initialize(java)
        @java = java
      end

      def run(proc, config, sample)
        config = Bridge::DataSourceBridge.wrap(config)
        sample = Buffer.wrap(sample.to_java_bytes)
        next_config = @java.guess(proc, config, sample)
        return Bridge::DataSourceBridge.wrap(next_config)
      end
    end
  end
end
