require 'helper'
require_relative "../mock_page_out.rb"

class PageBuilderTest < ::Test::Unit::TestCase
  setup do
    any_instance_of(Embulk::PageBuilder) do |klass|
      stub(klass).task {
        Embulk::DataSource.new({}).load_config(Embulk::Java::DynamicPageBuilder::BuilderTask)
      }
    end
    @mock_pageout = MockPageOut.new
    @page_builder = Embulk::PageBuilder.new(schema, @mock_pageout)
  end

  test "add" do
    input_records = [
      [{"foo" => {"FOO" => "FOO"}}, "str", 42],
      [{"bar" => [1,2,3]}, "S", -99],
    ]
    input_records.each do |record|
      @page_builder.add record
    end
    @page_builder.finish

    added_records = @mock_pageout.pages.map do |page|
      reader = Embulk::Page.new(page.to_java, schema)
      reader.to_a
    end

    assert_equal [input_records], added_records
  end

  test "column" do
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::JsonColumnSetter", @page_builder.column(0).class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::StringColumnSetter", @page_builder.column(schema[1]).class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::LongColumnSetter", @page_builder.column("col_long").class.to_s
    begin
      @page_builder.column("col_unknown")
      assert false, "column(unknown) should raise error"
    rescue Java::OrgEmbulkSpiUtil::DynamicColumnNotFoundException
      assert true
    end
  end

  test "[]" do
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::JsonColumnSetter", @page_builder[0].class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::StringColumnSetter", @page_builder[schema[1]].class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::LongColumnSetter", @page_builder["col_long"].class.to_s
    assert_nothing_raised do
      @page_builder["col_unknown"]
    end
  end

  test "column_or_skip" do
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::JsonColumnSetter", @page_builder.column_or_skip(0).class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::StringColumnSetter", @page_builder.column_or_skip(schema[1]).class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::LongColumnSetter", @page_builder.column_or_skip("col_long").class.to_s

    assert_equal "Java::OrgEmbulkSpiUtilDynamic::SkipColumnSetter", @page_builder.column_or_skip(-1).class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::SkipColumnSetter", @page_builder.column_or_skip(schema.length + 1).class.to_s
    assert_equal "Java::OrgEmbulkSpiUtilDynamic::SkipColumnSetter", @page_builder.column_or_skip("col_unknown").class.to_s
  end

  data do
    [
      ["add", [:add!, :add_record]],
      ["flush", [:flush, :flush]],
      ["finish", [:finish, :finish]],
      ["close", [:close, :close]],
    ]
  end
  test "delegated methods" do |data|
    ruby_method, java_method = data
    java_mock = mock()
    @page_builder.instance_variable_set(:@page_builder, java_mock)

    mock(java_mock, java_method)
    @page_builder.send(ruby_method)
  end

  private

  def schema
    Embulk::Schema.new([
      Embulk::Column.new(0, "col_json", :json, nil),
      Embulk::Column.new(1, "col_str", :string, nil),
      Embulk::Column.new(2, "col_long", :long, nil),
    ])
  end
end
