module Embulk

  require 'embulk/config_hash'

  class TaskConfig < ConfigHash
    def initialize(exec, hash={})
      @java_exec = exec
      super()
      merge!(hash)
      @columns = []
      @processor_count = 0
    end

    attr_reader :java_exec  # TODO wrap in Bridge::ProcTask?

    attr_accessor :columns
    attr_accessor :processor_count
  end

end
