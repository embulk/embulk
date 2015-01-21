bundle_path = ENV['EMBULK_BUNDLE_PATH'].to_s
bundle_path = nil if bundle_path.empty?

# search -b, --bundle BUNDLE_DIR
bundle_path_index = ARGV.find_index {|arg| arg == '-b' || arg == '--bundle' }
if bundle_path_index
  bundle_path = ARGV.slice!(bundle_path_index, 2)[1]
end

if bundle_path
  # use bundler installed at bundle_path
  ENV['GEM_HOME'] = File.expand_path File.join(bundle_path, Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
  ENV['GEM_PATH'] = ''
  Gem.clear_paths  # force rubygems to reload GEM_HOME

  ENV['BUNDLE_GEMFILE'] = File.expand_path File.join(bundle_path, "Gemfile")
  require 'bundler'
  Bundler.load.setup_environment
  # since here, `require` may load files of different (newer) embulk versions
  # especially following 'embulk/command/embulk_run'.

  $LOAD_PATH << File.expand_path(bundle_path)  # for local plugins
end

begin
  require 'embulk/command/embulk_run'
rescue LoadError
  require_relative 'embulk_run'
end

Embulk.run(ARGV)
