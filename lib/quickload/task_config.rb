module QuickLoad

  require 'quickload/config_hash'

  class TaskConfig < ConfigHash
    def initialize(proc, hash={})
      @java_proc = proc
      super()
      merge!(hash)
      @columns = []
      @processor_count = 0
    end

    attr_reader :java_proc  # TODO wrap in Bridge::ProcTask?

    attr_accessor :columns
    attr_accessor :processor_count
  end

end
