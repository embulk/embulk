module Embulk
  CORE_VERSION = Java::org.embulk.EmbulkVersion::VERSION

  # Converts the original Java-style version string to Ruby-style.
  # E.g., "0.9.0-SNAPSHOT" (in Java) is converted to "0.9.0.snapshot" in Ruby.
  CORE_VERSION_IN_RUBY_GEM_STYLE = ::String.new(CORE_VERSION).tr('-', '.').downcase
  private_constant :CORE_VERSION_IN_RUBY_GEM_STYLE

  begin
    require 'embulk/gem_version'
  rescue LoadError => e
    raise LoadError, "[Internal Error] This embulk.gem is not properly built with embulk/gem_version.rb to declare its own version."
  end

  begin
    GEM_VERSION = GEM_VERSION_EMBEDDED
  rescue NameError => e
    raise LoadError, "[Internal Error] This embulk.gem does not contain its own version defined properly."
  end

  if GEM_VERSION != CORE_VERSION_IN_RUBY_GEM_STYLE
    # "embulk/logger" cannot be used because embulk/version.rb is loaded even before embulk/logger.rb.
    STDERR.puts "*******************************************************************************************"
    STDERR.puts "Running Embulk version (#{CORE_VERSION}) does not match the installed embulk.gem version (#{GEM_VERSION})."
    STDERR.puts ""
    STDERR.puts "If you use Embulk v0.9.* without Bundler:"
    STDERR.puts "   Uninstall embulk.gem from your Gem path."
    STDERR.puts "   An embulk.gem-equivalent should be embedded in your Embulk's core JAR of v0.9.*."
    STDERR.puts ""
    STDERR.puts "If you use Embulk v0.9.* with Bundler:"
    STDERR.puts "   Try updating your Gemfile as below:"
    STDERR.puts "     gem 'embulk', '< 0.10'"
    STDERR.puts "   Bundler will find the embulk.gem-equivalent embedded in your Embulk's core JAR of v0.9.*."
    STDERR.puts ""
    STDERR.puts "If you use Embulk v0.10.*:"
    STDERR.puts "   Be aware that v0.10.* is an unstable development series. If you are aware of that,"
    STDERR.puts "   upgrade it to the latest v0.10.*, and use exactly the same version of embulk.gem."
    STDERR.puts "   In case you use Bundler, your Gemfile should have 'embulk' as below:"
    STDERR.puts "     gem 'embulk', '0.10.XX'  # Exactly the same version of your Embulk's core JAR."
    STDERR.puts ""
    STDERR.puts "If you use Embulk v0.8.* or earlier:"
    STDERR.puts "   Update to the latest v0.9.*. v0.8 or earlier are deprecated."
    STDERR.puts "*******************************************************************************************"
    raise LoadError, "Running Embulk version (#{CORE_VERSION}) does not match the installed embulk.gem version (#{GEM_VERSION})."
  end

  VERSION = GEM_VERSION
end
