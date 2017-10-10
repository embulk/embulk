module Embulk::Guess
  require 'embulk/column'
  require 'embulk/guess/time_format_guess'

  module SchemaGuess
    class TimestampTypeMatch < String
      def initialize(format)
        super("timestamp")
        @format = format
      end

      attr_reader :format
    end

    class << self
      def from_hash_records(array_of_hash)
        array_of_hash = Array(array_of_hash)
        if array_of_hash.empty?
          raise "SchemaGuess Can't guess schema from no records"
        end
        column_names = array_of_hash.first.keys
        samples = array_of_hash.to_a.map {|hash| column_names.map {|name| hash[name] } }
        from_array_records(column_names, samples)
      end

      def from_array_records(column_names, samples)
        column_types = types_from_array_records(samples)
        columns = column_types.zip(column_names).map do |(type,name)|
          hash = {name: name, type: type.to_sym}
          hash[:format] = type.format if type.is_a?(TimestampTypeMatch)
          Embulk::Column.new(hash)
        end
        return Embulk::Schema.new(columns)
      end

      # TODO this method will be private once guess/csv is refactored
      def types_from_array_records(samples)
        columnar_types = []
        samples.each do |record|
          record.each_with_index {|value,i| (columnar_types[i] ||= []) << guess_type(value) }
        end
        columnar_types.map {|types| merge_types(types) }
      end

      private

      def guess_type(str)
        if str.is_a?(Hash) || str.is_a?(Array)
          return "json"
        end
        str = str.to_s

        if TRUE_STRINGS[str] || FALSE_STRINGS[str]
          return "boolean"
        end

        if TimeFormatGuess.guess(str)
          return TimestampTypeMatch.new(str)
        end

        if str.to_i.to_s == str
          return "long"
        end

        # Introduce a regular expression to make better suggestion to double type. It refers to Guava 21.0's regular
        # expression in Doubles#fpPattern() but, there're difference as following:
        # * It intentionaly rejects float values when they start with "0" like "001.0", "010.01". "0.1" is ok.
        # * It doesn't support hexadecimal representation. It could be improved more later.
        if str =~ /^[+-]?(NaN|Infinity|([1-9]\d*|0)(\.\d+)([eE][+-]?\d+)?[fFdD]?)$/
          return "double"
        end

        if str.empty?
          return nil
        end

        begin
          JSON.parse(str)
          return "json"
        rescue
        end

        return "string"
      end

      def merge_types(types)
        t = types.inject(nil) {|r,t| merge_type(r,t) } || "string"
        if t.is_a?(TimestampTypeMatch)
          format = TimeFormatGuess.guess(types.map {|type| type.is_a?(TimestampTypeMatch) ? type.format : nil }.compact)
          return TimestampTypeMatch.new(format)
        else
          return t
        end
      end

      # taken from CsvParserPlugin.TRUE_STRINGS
      TRUE_STRINGS = Hash[%w[
        true True TRUE
        yes Yes YES
        t T y Y
        on On ON
      ].map {|k| [k, true] }]

      # When matching to false string, then retrun 'true'
      FALSE_STRINGS = Hash[%w[
        false False FALSE
        no No NO
        f F n N
        off Off OFF
      ].map {|k| [k, true] }]

      TYPE_COALESCE = Hash[{
        long: :double,
        boolean: :long,
        timestamp: :long,  # TimeFormatGuess matches with digits without delimiters
      }.map {|k,v|
        [[k.to_s, v.to_s].sort, v.to_s]
      }]

      def merge_type(type1, type2)
        if type1 == type2
          type1
        elsif type1.nil? || type2.nil?
          type1 || type2
        else
          TYPE_COALESCE[[type1, type2].sort] || "string"
        end
      end
    end
  end
end
