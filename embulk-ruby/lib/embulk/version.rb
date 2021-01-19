module Embulk
  # Converts the original Java-style version string to Ruby-style.
  # E.g., "0.9.0-SNAPSHOT" (in Java) is converted to "0.9.0.snapshot" in Ruby.
  VERSION = ::String.new(Java::org.embulk.EmbulkVersion::VERSION).tr('-', '.').downcase
end
