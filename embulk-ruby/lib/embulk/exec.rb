
module Embulk
  module Exec
    def self.preview?
      Java::SPI::Exec.isPreview
    end
  end
end
