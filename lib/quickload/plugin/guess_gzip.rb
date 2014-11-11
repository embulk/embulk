module QuickLoad
  require 'quickload/java_imports'
  require 'quickload/bridge/buffer'
  require 'quickload/bridge/data_source'

  class ParserGuessPluginBridge
    java_implements ParserGuessPlugin

    #def initialize(instance)
    #  @instance = instance
    #end
    def initialize
      @instance = self
    end

    def guess(proc, config, sample)
      sample = Bridge::BufferBridge.to_str(sample)
      config = Bridge::DataSourceBridge.wrap(config)
      next_config = @instance.run(proc, config, sample)
      return Bridge::DataSourceBridge.to_java_next_config(next_config)
    end

    Plugin.register_guess('gzip', self)

    GZIP_HEADER = "\x1f\x8b".force_encoding('ASCII-8BIT').freeze

    def run(proc, config, sample)
      if sample[0,2] == GZIP_HEADER
        return {"file_decoders" => [{"type" => "gzip"}]}
      end
      return nil
    end
  end
end
