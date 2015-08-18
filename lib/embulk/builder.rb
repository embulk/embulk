module Embulk

  require 'embulk/column'

  class Builder
    def self.add(name, type_str_or_sym)
      Column.new(nil, name, type_str_or_sym.to_sym)
    end

    def self.build(column_config)
      columns = []
      column_config.each do |column|
        name = column["name"]
        type = column["type"].to_sym

        columns << Column.new(nil, name, type)
      end
    end
  end

end
