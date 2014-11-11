module QuickLoad
  require 'quickload/java_imports'
  require 'quickload/bridge/buffer'

  class ParserGuessPluginBridge
    java_implements ParserGuessPlugin

    #def initialize(instance)
    #  @instance = instance
    #end
    def initialize
      @instance = self
    end

    def guess(proc, config, sample)
      sample = QuickLoad::Bridge::BufferBridge.to_str(sample)
      @instance.run(proc, config, sample)
    end

    Plugin.register_guess('gzip', self)

    GZIP_HEADER = "\x1f\x8b".force_encoding('ASCII-8BIT').freeze

    def run(proc, config, sample)
      if sample[0,2] == GZIP_HEADER
        # TODO
        {"parser" => {"file_decoders" => [{"type" => "gzip"}]}}
        return NextConfig.new
      end
      NextConfig.new
    end
  end
end
