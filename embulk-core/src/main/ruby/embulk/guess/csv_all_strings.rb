module Embulk
  module Guess
    require 'embulk/guess/csv'

    class CsvAllStringsGuessPlugin < CsvGuessPlugin
      Plugin.register_guess("csv_all_strings", self)

      def new_column(name, type)
        {"name" => name, "type" => "string"}
      end
    end
  end
end
