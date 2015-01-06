module Embulk
  if RUBY_PLATFORM =~ /java/i
    def self.java?
      true
    end
  else
    def self.java?
      false
    end
  end

  require 'embulk/error'
  require 'embulk/plugin'
  require 'embulk/buffer'
  require 'embulk/data_source'
end
