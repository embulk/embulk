module Embulk

  require 'embulk/column'
  require 'msgpack'

  class Schema < Array
    def initialize(columns)
      columns = columns.map.with_index {|c,index|
        if c.index && c.index != index
          # TODO ignore this error?
          raise "Index of column '#{c.name}' is #{c.index} but it is at column #{index}."
        end
        Column.new(index, c.name, c.type, c.format)
      }
      super(columns)

      record_reader_script =
        "lambda do |reader|\n" <<
        "record = []\n"
      each do |column|
        idx = column.index
        column_script =
          "if reader.isNull(#{idx})\n" <<
          "record << nil\n" <<
          "else\n" <<
          case column.type
          when :boolean
            "record << reader.getBoolean(#{idx})"
          when :long
            "record << reader.getLong(#{idx})"
          when :double
            "record << reader.getDouble(#{idx})"
          when :string
            "record << reader.getString(#{idx})"
          when :timestamp
            # Constructing through Java::org.jruby.RubyTime instead of constructing Ruby's Time directly
            # as Ruby's Time cannot be constructed from nanoseconds as of Ruby 2.4. Ruby's Time might be
            # extended independently from Java::org.jruby.RubyTime, for example, to_msgpack.
            # Java::org.jruby.RubyTime is converted to Ruby's Time by gmtime() as a result.
            # TODO: Replace to Ruby's Time.at(seconds, nanoseconds, :nsec) available from Ruby 2.5.0.
            # http://ruby-doc.org/core-2.5.0/Time.html#method-c-at
            "record << (java_timestamp = reader.getTimestamp(#{idx}); Java::org.jruby.RubyTime.newTime(JRuby.runtime, Java::org.joda.time.DateTime.new((java_timestamp.getEpochSecond() * 1000) + (java_timestamp.getNano() / 1000000)), (java_timestamp.getNano() % 1000000)).gmtime())"
          when :json
            "record << MessagePack.unpack(String.from_java_bytes((::Java::org.msgpack.core.MessagePack.newDefaultBufferPacker()).packValue(reader.getJson(#{idx})).toMessageBuffer().toByteArray()))"
          else
            raise "Unknown type #{column.type.inspect}"
          end <<
          "end\n"
        record_reader_script << column_script << "\n"
      end
      record_reader_script << "record\n"
      record_reader_script << "end"
      @record_reader = eval(record_reader_script)

      record_writer_script = "lambda do |builder,record|\n"
      record_writer_script << "java_timestamp_class = ::Embulk::Java::Timestamp\n"
      each do |column|
        idx = column.index
        column_script =
          "if record[#{idx}].nil?\n" <<
          "builder.setNull(#{idx})\n" <<
          "else\n" <<
          case column.type
          when :boolean
            "builder.setBoolean(#{idx}, record[#{idx}])"
          when :long
            "builder.setLong(#{idx}, record[#{idx}])"
          when :double
            "builder.setDouble(#{idx}, record[#{idx}])"
          when :string
            "builder.setString(#{idx}, record[#{idx}])"
          when :timestamp
            "builder.setTimestamp(#{idx}, case record[#{idx}] when Java::org.embulk.spi.time.Timestamp then record[#{idx}] when Java::java.time.Instant then Java::org.embulk.spi.time.Timestamp.ofInstant(record[#{idx}]) when Time then Java::org.embulk.spi.time.Timestamp.ofEpochSecond(record[#{idx}].to_i, record[#{idx}].nsec) end)"
          when :json
            "builder.setJson(#{idx}, ::Java::org.msgpack.core.MessagePack.newDefaultUnpacker(MessagePack.pack(record[#{idx}]).to_java_bytes).unpackValue())"
          else
            raise "Unknown type #{column.type.inspect}"
          end <<
          "end\n"
        record_writer_script << column_script << "\n"
      end
      record_writer_script << "builder.addRecord\n"
      record_writer_script << "end"
      @record_writer = eval(record_writer_script)

      @names = map {|c| c.name }
      @types = map {|c| c.type }

      freeze
    end

    attr_reader :names, :types

    def read_record(page_reader)
      @record_reader.call(page_reader)
    end

    def write_record(page_builder, record)
      @record_writer.call(page_builder, record)
    end

    def self.from_java(java_schema)
      new java_schema.getColumns.map {|column| Column.from_java(column) }
    end

    def to_java
      columns = self.map {|column| column.to_java }
      Java::Schema.new(columns)
    end
  end

end
