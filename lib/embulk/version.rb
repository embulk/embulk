# TODO(v2)[#562]: Remove this file.
# https://github.com/embulk/embulk/issues/562
module Embulk
  @@warned = false

  VERSION_INTERNAL = '0.8.36'

  DEPRECATED_MESSAGE = 'Embulk::VERSION in (J)Ruby is deprecated. Use org.embulk.EmbulkVersion::VERSION instead. If this message is from a plugin, please tell this to the author of the plugin!'
  def self.const_missing(name)
    if name == :VERSION
      unless @@warned
        if Embulk.method_defined?(:logger)
          Embulk.logger.warn(DEPRECATED_MESSAGE)
          @@warned = true
        else
          STDERR.puts(DEPRECATED_MESSAGE)
        end
      end
      return VERSION_INTERNAL
    else
      super
    end
  end
end
