bundle_path = ENV['EMBULK_BUNDLE_PATH'].to_s
bundle_path = nil if bundle_path.empty?

# Search for -b or --bundle, and remove it.
bundle_option_index = ARGV.find_index {|arg| arg == '-b' || arg == '--bundle' }
if bundle_option_index
  ARGV.slice!(bundle_option_index, 2)[1]
end

if bundle_path
  # In the selfrun script:
  # ENV['EMBULK_BUNDLE_PATH']: set through '-b' | '--bundle', or inherit from the runtime environment
  # ENV['BUNDLE_GEMFILE']: set for "ENV['EMBULK_BUNDLE_PATH']/Gemfile"
  # ENV['GEM_HOME']: unset
  # ENV['GEM_PATH']: unset

  # bundler is included in embulk-core.jar
  Gem.clear_paths
  require 'bundler'

  Bundler.load.setup_environment
  require 'bundler/setup'
  # since here, `require` may load files of different (newer) embulk versions
  # especially following 'embulk/command/embulk_main'.

  # add bundle directory path to load local plugins at ./embulk
  $LOAD_PATH << File.expand_path(bundle_path)

  begin
    require 'embulk/command/embulk_main'
  rescue LoadError
    $LOAD_PATH << File.expand_path('../../', File.dirname(__FILE__))
    require 'embulk/command/embulk_main'
  end

else
  # In the selfrun script:
  # ENV['EMBULK_BUNDLE_PATH']: unset
  # ENV['BUNDLE_GEMFILE']: unset
  # ENV['GEM_HOME']: set for "~/.embulk/jruby/${ruby-version}"
  # ENV['GEM_PATH']: set for ""

  Gem.clear_paths  # force rubygems to reload GEM_HOME

  $LOAD_PATH << File.expand_path('../../', File.dirname(__FILE__))
  require 'embulk/command/embulk_main'
end
