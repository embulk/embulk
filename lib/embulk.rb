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

  require 'embulk/logger'
  require 'embulk/error'
  require 'embulk/buffer'
  require 'embulk/data_source'
  require 'embulk/plugin'
end
