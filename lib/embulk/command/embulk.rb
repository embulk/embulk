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

require 'embulk/command/embulk_run'
Embulk.run(load_paths, subcmd.to_sym, ARGV, options)

