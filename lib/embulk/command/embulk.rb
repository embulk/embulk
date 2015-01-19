require 'optparse'

define_singleton_method(:usage) do |message|
  STDERR.puts "usage: <command> [--options]"
  STDERR.puts "commands:"
  STDERR.puts "   bundle  [--options] [new directory]"
  STDERR.puts "   run     [--options] <config.yml>"
  STDERR.puts "   preview [--options] <config.yml>"
  STDERR.puts "   guess   [--options] <partial-config.yml>  | tee config.yml"
  STDERR.puts ""
  if message
    STDERR.puts "error: #{message}"
  else
    STDERR.puts "Use \`<command> --help\` to see description of the commands."
  end
  exit 1
end

i = ARGV.find_index {|arg| arg !~ /^\-/ }
usage nil unless i
subcmd = ARGV.slice!(i)

op = OptionParser.new
conf = {}

case subcmd.to_sym
when :bundle
  op.banner = "Usage: bundle [new directory]"
  args = 0..1

when :run
  op.banner = "Usage: run [--options] <config.yml>"
  op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
    conf[:bundle_path] = path
  end
  op.on('-G', '--with-guess', TrueClass) do |b|
    conf[:with_guess] = true
  end
  op.on('-P', '--with-preview', TrueClass) do |b|
    conf[:with_preview] = true
  end
  op.on('-I', '--load-path PATH', 'Add $LOAD_PATH for plugin scripts') do |load_path|
    $LOAD_PATH << File.expand_path(load_path)
  end
  args = 1..1

when :preview
  op.banner = "Usage: preview [--options] <config.yml>"
  op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
    conf[:bundle_path] = path
  end
  op.on('-I', '--load-path', 'Add $LOAD_PATH for plugin scripts') do |load_path|
    $LOAD_PATH << File.expand_path(load_path)
  end
  args = 1..1

when :guess
  op.banner = "Usage: guess [--options] <partial-config.yml>"
  op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
    conf[:bundle_path] = path
  end
  op.on('-I', '--load-path', 'Add $LOAD_PATH for plugin scripts') do |load_path|
    $LOAD_PATH << File.expand_path(load_path)
  end
  args = 1..1

else
  usage "Unknown subcommand #{subcmd.dump}."
end

begin
  op.parse!(ARGV)
  unless args.include?(ARGV.length)
    usage nil
  end
rescue => e
  usage e.to_s
end

def setup_gem_paths(path)
  ENV['GEM_HOME'] = File.expand_path "#{path}/#{Gem.ruby_engine}/#{RbConfig::CONFIG['ruby_version']}"
  ENV['GEM_PATH'] = ''
  ENV.delete 'BUNDLE_GEMFILE'
  Gem.clear_paths  # force rubygems to reload GEM_HOME
end

case subcmd.to_sym
when :bundle
  path = ARGV[0] || "."

  require 'fileutils'
  require 'rubygems/gem_runner'
  setup_gem_paths(path)

  unless File.exists?(path)
    puts "Initializing #{path}..."
    FileUtils.mkdir_p File.dirname(path)
    tmpl = File.expand_path("#{File.dirname(__FILE__)}/../../../templates/bundle")
    FileUtils.cp_r tmpl, path
    begin
      Gem::GemRunner.new.run %w[install bundler]
    rescue Gem::SystemExitException => e
      if e.exit_code != 0
        FileUtils.rm_rf path
        raise e
      end
    end
  end

  Dir.chdir(path) do
    Gem.clear_paths  # force rubygems to reload GEM_HOME
    require 'bundler'
    require 'bundler/friendly_errors'
    require 'bundler/cli'
    Bundler.with_friendly_errors do
      # run > bundle install
      Bundler::CLI.start(%w[install], debug: true)
    end
  end

else
  if bundle_path = conf[:bundle_path]
    setup_gem_paths(bundle_path)
    require 'bundler'  # bundler is installed at bundle_path
    Bundler.load.setup_environment
  else
    $LOAD_PATH << File.expand_path('../..', File.dirname(__FILE__))
  end

  require 'embulk/command/embulk_home'  # set EMBULK_HOME
  require 'java'
  require 'json'

  begin
    java.lang.Class.forName('org.embulk.cli.Runner')
  rescue java.lang.ClassNotFoundException
    # load classpath
    classpath_dir = File.join(ENV['EMBULK_HOME'], 'classpath')
    jars = Dir.entries(classpath_dir).select {|f| f =~ /\.jar$/ }.sort
    jars.each do |jar|
      require File.join(classpath_dir, jar)
    end
  end

  org.embulk.cli.Runner.new(conf.to_json).main(subcmd, ARGV.to_java(:string))
end
