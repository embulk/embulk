
module Embulk
  module Exec
    def self.preview?
      Java::Exec.isPreview
    end
  end
end
