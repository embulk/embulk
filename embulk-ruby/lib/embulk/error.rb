
module Embulk
  # ConfigError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues ConfigError.
  class ConfigError < Java::Config::ConfigException
    # :call-seq:
    #   ConfigError.new()
    #   ConfigError.new(string)
    #
    # Returns the new +ConfigError+ object.
    #
    # The signature of +ConfigError.new+ accepts any argument. But in fact, it
    # accepts only <tt>ConfigError.new()</tt> and <tt>ConfigError.new(String)</tt>.
    # It raises <tt>Error: org.jruby.exceptions.Argument</tt> otherwise. It has
    # been like this since \Embulk v0.10.38 because a subclass of a Java class is
    # a full Java class as of JRuby 9.3, which introduced some restrictions.
    #
    # See also:
    # - https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class
    # - https://github.com/jruby/jruby/issues/7221
    #
    # ==== History
    #
    # +ConfigError.initialize+ receives only a variable-length argument list
    # (rest parameters) since \Embulk v0.10.38. It is to satisfy restrictions
    # as of JRuby 9.3.
    #
    # It was <tt>ConfigError.initialize(message=nil)</tt> before v0.10.38.
    # It switched over to call <tt>super()</tt> (<tt>ConfigException()</tt>),
    # or <tt>super(String)</tt> (<tt>ConfigException(String)</tt>) based on
    # +message+. It was because Ruby does not allow method overloading, and
    # then, +ConfigError+ can have only a single +initialize+ method.
    #
    # On the other hand, JRuby 9.3+ does not allow +initialize+ to contain
    # multiple +super+ calls in it. Switching the superclass constructor is
    # no longer accepted in JRuby 9.3+.
    #
    # To keep compatibility from caller's viewpoint, +ConfigError.initialize+
    # receives only a variable-length argument list (<tt>*arguments</tt>), and
    # calls the superclass constructor with +super+ (without parenthesis). It
    # allows <tt>ConfigError.new()</tt> and <tt>ConfigError.new(String)</tt>
    # to call an appropriate superclass constructor for each.
    #
    # However, +ConfigError.new+ now accepts any argument undesirably. It
    # raises an unexpected <tt>Error: org.jruby.exceptions.ArgumentError</tt>
    # when +ConfigError.new+ is called with an unexpected argument.
    def initialize(*arguments)
      super
    end
  end

  # DataError is not a ::StandardError but is a java.lang.RuntimeException.
  # "rescue => e" can rescues DataError.
  class DataError < Java::SPI::DataException
    # :call-seq:
    #   DataError.new()
    #   DataError.new(string)
    #
    # Returns the new +DataError+ object.
    #
    # The signature of +DataError.new+ accepts any argument. But in fact, it
    # accepts only <tt>DataError.new()</tt> and <tt>DataError.new(String)</tt>.
    # It raises <tt>Error: org.jruby.exceptions.Argument</tt> otherwise. It has
    # been like this since \Embulk v0.10.38 because a subclass of a Java class is
    # a full Java class as of JRuby 9.3, which introduced some restrictions.
    #
    # See also:
    # - https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class
    # - https://github.com/jruby/jruby/issues/7221
    #
    # ==== History
    #
    # +DataError.initialize+ receives only a variable-length argument list
    # (rest parameters) since \Embulk v0.10.38. It is to satisfy restrictions
    # as of JRuby 9.3.
    #
    # It was <tt>DataError.initialize(message=nil)</tt> before v0.10.38.
    # It switched over to call <tt>super()</tt> (<tt>ConfigException()</tt>),
    # or <tt>super(String)</tt> (<tt>ConfigException(String)</tt>) based on
    # +message+. It was because Ruby does not allow method overloading, and
    # then, +DataError+ can have only a single +initialize+ method.
    #
    # On the other hand, JRuby 9.3+ does not allow +initialize+ to contain
    # multiple +super+ calls in it. Switching the superclass constructor is
    # no longer accepted in JRuby 9.3+.
    #
    # To keep compatibility from caller's viewpoint, +DataError.initialize+
    # receives only a variable-length argument list (<tt>*arguments</tt>), and
    # calls the superclass constructor with +super+ (without parenthesis). It
    # allows <tt>DataError.new()</tt> and <tt>DataError.new(String)</tt>
    # to call an appropriate superclass constructor for each.
    #
    # However, +DataError.new+ now accepts any argument undesirably. It
    # raises an unexpected <tt>Error: org.jruby.exceptions.ArgumentError</tt>
    # when +DataError.new+ is called with an unexpected argument.
    def initialize(*arguments)
      super
    end
  end

  class PluginLoadError < ConfigError
  end
end
