module Embulk
  require 'embulk/version'  # 'embulk/version' is loaded in the very beginning.

  # this file is required before loading embulk-core.jar

  require 'logger'

  class Logger
    def initialize(*args)
      if args.length == 1
        a = args[0]
        if a.is_a?(Adapter)
          @logger = a
        elsif a.is_a?(::Logger)
          @logger = StandardLoggerAdapter.new(a)
        elsif RUBY_PLATFORM =~ /java/i && (org.slf4j.Logger rescue nil) && a.is_a?(org.slf4j.Logger)
          @logger = Slf4jAdapter.new(a)
        else
          @logger = StandardLoggerAdapter.new(*args)
        end
      else
        @logger = StandardLoggerAdapter.new(*args)
      end
    end

    module Adapter
    end

    def error(message=nil, &block) @logger.error(message, &block) end
    def warn(message=nil, &block) @logger.warn(message, &block) end
    def info(message=nil, &block) @logger.info(message, &block) end
    def debug(message=nil, &block) @logger.debug(message, &block) end
    def trace(message=nil, &block) @logger.trace(message, &block) end

    def error?() @logger.error? end
    def warn?() @logger.warn? end
    def info?() @logger.info? end
    def debug?() @logger.debug? end
    def trace?() @logger.trace? end
  end

  class StandardLoggerAdapter < ::Logger
    include Logger::Adapter

    def initialize(*args)
      super
      if RUBY_PLATFORM =~ /java/i
        self.formatter = lambda do |severity,datetime,progname,message|
          "#{datetime.strftime("%Y-%m-%d %H:%M:%S.%3N %z")} [#{severity}] (#{java.lang.Thread.currentThread.name}): #{message}\n"
        end
      else
        self.formatter = lambda do |severity,datetime,progname,message|
          "#{datetime.strftime("%Y-%m-%d %H:%M:%S.%3N %z")} [#{severity}]: #{message}\n"
        end
      end
    end

    def trace(message, &block)
      debug(message, &block)
    end

    def trace?
      debug?
    end
  end

  class Slf4jAdapter
    include Logger::Adapter

    def initialize(logger)
      @logger = logger
    end

    def error(message, &block)
      if block
        if @logger.isErrorEnabled
          @logger.error(block.call)
        end
      else
        @logger.error(message)
      end
    end

    def warn(message, &block)
      if block
        if @logger.isWarnEnabled
          @logger.warn(block.call)
        end
      else
        @logger.warn(message)
      end
    end

    def info(message, &block)
      if block
        if @logger.isInfoEnabled
          @logger.info(block.call)
        end
      else
        @logger.info(message)
      end
    end

    def debug(message, &block)
      if block
        if @logger.isDebugEnabled
          @logger.debug(block.call)
        end
      else
        @logger.debug(message)
      end
    end

    def trace(message, &block)
      if block
        if @logger.isTraceEnabled
          @logger.trace(block.call)
        end
      else
        @logger.trace(message)
      end
    end

    def fatal?
      @logger.isErrorEnabled()
    end

    def error?
      @logger.isErrorEnabled()
    end

    def warn?
      @logger.isWarnEnabled()
    end

    def debug?
      @logger.isDebugEnabled()
    end

    def trace?
      @logger.isTraceEnabled()
    end
  end

  def self.logger
    @@logger
  end

  def self.logger=(logger)
    @@logger = logger
  end

  # default logger
  @@logger = Logger.new(STDOUT)
end
