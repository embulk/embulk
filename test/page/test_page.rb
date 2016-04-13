require 'helper'
require_relative "../mock_page_out.rb"

class PageTest < ::Test::Unit::TestCase
  setup do
    any_instance_of(Embulk::PageBuilder) do |klass|
      stub(klass).task {
        Embulk::DataSource.new({}).load_config(Embulk::Java::DynamicPageBuilder::BuilderTask)
      }
    end
    @mock_pageout = MockPageOut.new
    @page_builder = Embulk::PageBuilder.new(schema, @mock_pageout)

    records.each do |record|
      @page_builder.add record
    end
    @page_builder.finish

    @page = @mock_pageout.pages.first
  end

  test "#schema" do
    reader = Embulk::Page.new(@page.to_java, schema)
    assert reader.schema == schema
  end

  test "#each to read records for each" do
    reader = Embulk::Page.new(@page.to_java, schema)
    reader.each_with_index do |record, i|
      assert record == records[i]
    end
  end

  test "Enumerable" do
    reader = Embulk::Page.new(@page.to_java, schema)
    assert reader.respond_to?(:each)
    assert reader.class.include?(Enumerable)
  end

  private

  def schema
    Embulk::Schema.new([
      Embulk::Column.new(0, "col_json", :json, nil),
      Embulk::Column.new(1, "col_str", :string, nil),
      Embulk::Column.new(2, "col_long", :long, nil),
    ])
  end

  def records
    [
      [{"foo" => {"FOO" => "FOO"}}, "str", 42],
      [{"bar" => [1,2,3]}, "S", -99],
    ]
  end
end

