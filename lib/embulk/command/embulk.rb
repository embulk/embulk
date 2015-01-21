require 'optparse'

define_singleton_method(:usage) do |message|
  STDERR.puts "usage: <command> [--options]"
  STDERR.puts "commands:"
  STDERR.puts "   bundle  [--options] [new directory]"
  STDERR.puts "   run     [--options] <config.yml>"
  STDERR.puts "   preview [--options] <config.yml>"
  STDERR.puts "   guess   [--options] <partial-config.yml> -o <output.yml>"
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

bundle_path = ENV['EMBULK_BUNDLE_PATH'].to_s
bundle_path = nil if bundle_path.empty?
load_paths = []
options = {}

op = OptionParser.new

case subcmd.to_sym
when :bundle
  op.banner = "Usage: bundle [new directory]"
  args = 0..1

when :run
  op.banner = "Usage: run [--options] <config.yml>"
  op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
    bundle_path = path
  end
  op.on('-I', '--load-path PATH', 'Add ruby script directory path or jar file path') do |load_path|
    load_paths << load_path
  end
  args = 1..1

when :preview
  op.banner = "Usage: preview [--options] <config.yml>"
  op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
    bundle_path = path
  end
  op.on('-I', '--load-path PATH', 'Add ruby script directory path or jar file path') do |load_path|
    load_paths << load_path
  end
  args = 1..1

when :guess
  op.banner = "Usage: guess [--options] <partial-config.yml>"
  op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
    bundle_path = path
  end
  op.on('-o', '--output PATH', 'Path to write the guessed config file') do |path|
    options[:guessOutput] = path
  end
  op.on('-I', '--load-path PATH', 'Add ruby script directory path or jar file path') do |load_path|
    load_paths << load_path
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
  ENV['GEM_HOME'] = File.expand_path File.join(path, Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
  ENV['GEM_PATH'] = ''
  Gem.clear_paths  # force rubygems to reload GEM_HOME
  ENV['BUNDLE_GEMFILE'] = File.expand_path File.join(path, "Gemfile")
end

def setup_load_paths(load_paths)
  load_paths.each do |load_path|
    if File.file?(load_path)
      # jar files
      require File.expand_path(load_path)
    else
      # ruby script directory
      $LOAD_PATH << File.expand_path(load_path)
    end
  end
end

case subcmd.to_sym
when :bundle
  path = ARGV[0] || "."

  require 'fileutils'
  require 'rubygems/gem_runner'
  setup_gem_paths(path)
  setup_load_paths(load_paths)

  unless File.exists?(path)
    puts "Initializing #{path}..."
    FileUtils.mkdir_p File.dirname(path)
    if __FILE__ =~ /^classpath:/
      # data is in jar
      resource_class = org.embulk.command.Runner.java_class
      # TODO get file list form the jar
      %w[.bundle/config embulk/input_example.rb embulk/output_example.rb examples/csv.yml examples/sample.csv.gz Gemfile Gemfile.lock].each do |file|
        url = resource_class.resource("/embulk/data/bundle/#{file}").to_s
        dst = File.join(path, file)
        FileUtils.mkdir_p File.dirname(dst)
        FileUtils.cp(url, dst)
      end
    else
      tmpl = File.join(File.dirname(__FILE__), '../data/bundle')
      FileUtils.cp_r tmpl, path
    end
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
  if bundle_path
    setup_gem_paths(bundle_path)
    require 'bundler'  # bundler is installed at bundle_path
    Bundler.load.setup_environment
    $LOAD_PATH << File.expand_path(bundle_path)  # for local plugins
    # since here, require '...' may load files of different (newer) embulk versions
    # especially following 'embulk/command/embulk_home'
  end
  require 'embulk/command/embulk_home'  # defines Embulk.home(dir)

  # for org.embulk.jruby.JRubyScriptingModule to require 'embulk/java/bootstrap'
  $LOAD_PATH << Embulk.home('lib')

  require 'java'
  require 'json'

  begin
    java.lang.Class.forName('org.embulk.command.Runner')
  rescue java.lang.ClassNotFoundException
    # load classpath
    classpath_dir = Embulk.home('classpath')
    jars = Dir.entries(classpath_dir).select {|f| f =~ /\.jar$/ }.sort
    jars.each do |jar|
      require File.join(classpath_dir, jar)
    end
  end

  setup_load_paths(load_paths)

  org.embulk.command.Runner.new(options.to_json).main(subcmd, ARGV.to_java(:string))
end
