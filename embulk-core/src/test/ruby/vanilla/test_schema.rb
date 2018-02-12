require 'time'

class SchemaTest < ::Test::Unit::TestCase
  def test_schema
    schema = ::Embulk::Schema.from_java(create_schema)
    page_reader = DummyPageReader.new("foobar", 1392207583, 381939141)
    records = schema.read_record(page_reader)
    assert_equal("foobar", records[0])
    expected_time = Time.at(Rational(1392207583381939141, 1000000000)).gmtime()
    assert_equal(expected_time.class, records[1].class)
    assert_equal(expected_time.to_i, records[1].to_i)
    assert_equal(expected_time.nsec, records[1].nsec)
  end

  def create_schema
    builder = Java::org.embulk.spi.Schema.builder()
    builder.add('name', Java::org.embulk.spi.type.Types::STRING)
    builder.add('time', Java::org.embulk.spi.type.Types::TIMESTAMP)
    builder.build()
  end
end

class DummyPageReader
  def initialize(name, epoch, nano)
    @name = Java::java.lang.String.new(name)
    @timestamp = Java::org.embulk.spi.time.Timestamp.ofEpochSecond(epoch, nano)
  end

  def isNull(index)
    return false
  end

  def getString(index)
    return @name
  end

  def getTimestamp(index)
    return @timestamp
  end
end
