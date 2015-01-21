require 'optparse'

define_singleton_method(:usage) do |message|
  STDERR.puts "usage: <command> [--options]"
  STDERR.puts "commands:"
  STDERR.puts "   bundle    [new directory]"
  STDERR.puts "   run       <config.yml>"
  STDERR.puts "   preview   <config.yml>"
  STDERR.puts "   guess     <partial-config.yml> -o <output.yml>"
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

if bundle_path
  setup_gem_paths(bundle_path)
  require 'bundler'  # bundler is installed at bundle_path
  Bundler.load.setup_environment
  $LOAD_PATH << File.expand_path(bundle_path)  # for local plugins
  # since here, `require` may load files of different (newer) embulk versions
  # especially following 'embulk/command/embulk_home'
end

begin
  require 'embulk/command/embulk_home'  # defines Embulk.home(dir)
rescue LoadError
  require_relative 'embulk_home'
end

# for org.embulk.jruby.JRubyScriptingModule to require 'embulk/java/bootstrap'
$LOAD_PATH << Embulk.home('lib')

# TODO move following script to embulk/command/xyz so that
#      self-update works

case subcmd.to_sym
when :bundle
  path = ARGV[0] || "."

  require 'fileutils'
  require 'rubygems/gem_runner'
  setup_load_paths(load_paths)

  unless File.exists?(path)
    puts "Initializing #{path}..."
    FileUtils.mkdir_p File.dirname(path)
    begin
      success = false

      # copy embulk/data/bundle/ directory
      if __FILE__ =~ /^classpath:/
        # data is in jar
        resource_class = org.embulk.command.Runner.java_class
        %w[.bundle/config embulk/input_example.rb embulk/output_example.rb examples/csv.yml examples/sample.csv.gz Gemfile Gemfile.lock].each do |file|  # TODO get file list from the jar
          url = resource_class.resource("/embulk/data/bundle/#{file}").to_s
          dst = File.join(path, file)
          FileUtils.mkdir_p File.dirname(dst)
          FileUtils.cp(url, dst)
        end
      else
        #tmpl = File.join(File.dirname(__FILE__), '../data/bundle')
        tmpl = File.join(Embulk.home('lib'), 'embulk', 'data', 'bundle')
        FileUtils.cp_r tmpl, path
      end

      # create bin/embulk
      bin_embulk_path = File.join(path, 'bin', 'embulk')
      FileUtils.mkdir_p File.dirname(bin_embulk_path)
      require 'embulk/command/embulk_generate_bin'  # defines Embulk.generate_bin
      File.open(bin_embulk_path, 'wb', 0755) {|f| f.write Embulk.generate_bin(bundle_path: :here) }

      # install bundler
      setup_gem_paths(path)
      Gem::GemRunner.new.run %w[install bundler]

      success = true
    rescue Gem::SystemExitException => e
      raise e if e.exit_code != 0
      success = true
    ensure
      FileUtils.rm_rf path unless success
    end
  else
    setup_gem_paths(path)
  end

  Dir.chdir(path) do
    require 'bundler'
    require 'bundler/friendly_errors'
    require 'bundler/cli'
    Bundler.with_friendly_errors do
      # run > bundle install
      Bundler::CLI.start(%w[install], debug: true)
    end
  end

else
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
