module Embulk
  module Guess
    class JsonGuessPlugin < GuessPlugin
      Plugin.register_guess('json', self)

      java_import 'com.google.common.collect.Lists'
      java_import 'java.io.ByteArrayInputStream'
      java_import 'org.embulk.spi.Exec'
      java_import 'org.embulk.spi.json.JsonParser'
      java_import 'org.embulk.spi.json.JsonParseException'
      java_import 'org.embulk.spi.util.FileInputInputStream'
      java_import 'org.embulk.spi.util.InputStreamFileInput'

      def guess(config, sample_buffer)
        return {} unless config.fetch("parser", {}).fetch("type", "json") == "json"

        # Use org.embulk.spi.json.JsonParser to respond to multi-line Json
        json_parser = new_json_parser(sample_buffer)
        one_json_parsed = false
        begin
          while (v = json_parser.next)
            # "v" needs to be JSON object type (isMapValue) because:
            # 1) Single-column CSV can be mis-guessed as JSON if JSON non-objects are accepted.
            # 2) JsonParserPlugin accepts only the JSON object type.
            raise JsonParseException.new("v must be JSON object type") unless v.isMapValue
            one_json_parsed = true
          end
        rescue JsonParseException
          # the exception is ignored
        end

        if one_json_parsed
          return {"parser" => {"type" => "json"}} # if JsonParser can parse even one JSON data
        else
          return {}
        end
      end

      private

      def new_json_parser(buffer)
        input_streams = Lists::newArrayList(ByteArrayInputStream.new(buffer.to_java_bytes))
        iterator_provider = InputStreamFileInput::IteratorProvider.new(input_streams)
        input = FileInputInputStream.new(InputStreamFileInput.new(Java::SPI::Exec.getBufferAllocator(), iterator_provider))
        input.nextFile
        JsonParser.new.open(input)
      end
    end
  end
end
