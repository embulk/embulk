
bundle_path = ENV['EMBULK_BUNDLE_PATH'].to_s
bundle_path = nil if bundle_path.empty?

# search -b, --bundle BUNDLE_DIR
bundle_path_index = ARGV.find_index {|arg| arg == '-b' || arg == '--bundle' }
if bundle_path_index
  bundle_path = ARGV.slice!(bundle_path_index, 2)[1]
end

if bundle_path
  # use bundler installed at bundle_path
  ENV['EMBULK_BUNDLE_PATH'] = bundle_path
  ENV['GEM_HOME'] = File.expand_path File.join(bundle_path, Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
  ENV['GEM_PATH'] = ''
  Gem.clear_paths  # force rubygems to reload GEM_HOME

  ENV['BUNDLE_GEMFILE'] = File.expand_path File.join(bundle_path, "Gemfile")

  begin
    require 'bundler'
  rescue LoadError => e
    raise "#{e}\nBundler is not installed. Did you run \`$ embulk bundle #{bundle_path}\` ?"
  end
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
  # default GEM_HOME is ~/.embulk/jruby/1.9/. If -b option is set,
  # GEM_HOME is already set by embulk/command/embulk_main.rb
  ENV.delete('EMBULK_BUNDLE_PATH')
  user_home = java.lang.System.properties["user.home"] || ENV['HOME']
  unless user_home
    raise "HOME environment variable is not set."
  end
  ENV['GEM_HOME'] = File.expand_path File.join(user_home, '.embulk', Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])

  jar, resource = __FILE__.split("!", 2)
  if resource
    embulk_lib = resource.split("/")[0..-3].join("/")
    ENV['GEM_PATH'] = "#{jar}!#{embulk_lib}/gems"
  else
    ENV.delete('GEM_PATH')
  end

  ENV.delete('BUNDLE_GEMFILE')
  Gem.clear_paths  # force rubygems to reload GEM_HOME

  $LOAD_PATH << File.expand_path('../../', File.dirname(__FILE__))
  require 'embulk/command/embulk_main'
end
