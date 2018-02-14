require 'time'

class SchemaTest < ::Test::Unit::TestCase
  def test_read_schema
    schema = ::Embulk::Schema.from_java(create_schema_for_read)
    page_reader = DummyPageReader.new("foobar", 1392207583, 381939141)
    records = schema.read_record(page_reader)
    assert_equal("foobar", records[0])
    expected_time = Time.at(Rational(1392207583381939141, 1000000000)).gmtime()
    assert_equal(expected_time.class, records[1].class)
    assert_equal(expected_time.to_i, records[1].to_i)
    assert_equal(expected_time.nsec, records[1].nsec)
  end

  def test_write_schema
    schema = ::Embulk::Schema.from_java(create_schema_for_write)
    page_builder = DummyPageBuilder.new
    record = [
      Time.at(Rational(1492207583281939141, 1000000000)).gmtime(),
      Java::org.jruby.RubyTime.newTime(JRuby.runtime,
                                       Java::org.joda.time.DateTime.new(1592207583 * 1000),
                                       181939141),
      Java::org.embulk.spi.time.Timestamp.ofEpochSecond(1292207583, 191939141),
      Java::java.time.Instant.ofEpochSecond(1192207583, 171939141),
    ]
    schema.write_record(page_builder, record)

    assert_equal(Java::org.embulk.spi.time::Timestamp, page_builder.get_records[0][0].class)
    assert_equal(1492207583, page_builder.get_records[0][0].getEpochSecond())
    assert_equal(281939141, page_builder.get_records[0][0].getNano())
    assert_equal(Java::org.embulk.spi.time::Timestamp, page_builder.get_records[0][1].class)
    assert_equal(1592207583, page_builder.get_records[0][1].getEpochSecond())
    assert_equal(181939141, page_builder.get_records[0][1].getNano())
    assert_equal(Java::org.embulk.spi.time::Timestamp, page_builder.get_records[0][2].class)
    assert_equal(1292207583, page_builder.get_records[0][2].getEpochSecond())
    assert_equal(191939141, page_builder.get_records[0][2].getNano())
    assert_equal(Java::org.embulk.spi.time::Timestamp, page_builder.get_records[0][3].class)
    assert_equal(1192207583, page_builder.get_records[0][3].getEpochSecond())
    assert_equal(171939141, page_builder.get_records[0][3].getNano())
  end

  def create_schema_for_read
    builder = Java::org.embulk.spi.Schema.builder()
    builder.add('name', Java::org.embulk.spi.type.Types::STRING)
    builder.add('time', Java::org.embulk.spi.type.Types::TIMESTAMP)
    builder.build()
  end

  def create_schema_for_write
    builder = Java::org.embulk.spi.Schema.builder()
    builder.add('time1', Java::org.embulk.spi.type.Types::TIMESTAMP)
    builder.add('time2', Java::org.embulk.spi.type.Types::TIMESTAMP)
    builder.add('time3', Java::org.embulk.spi.type.Types::TIMESTAMP)
    builder.add('time4', Java::org.embulk.spi.type.Types::TIMESTAMP)
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

class DummyPageBuilder
  def initialize
    @record_to_add = []
    @all_records = []
  end

  def setString(index, value)
    @record_to_add[index] = value
  end

  def setTimestamp(index, value)
    @record_to_add[index] = value
  end

  def addRecord
    @all_records << @record_to_add
    @record_to_add = []
  end

  def get_records
    @all_records
  end
end
