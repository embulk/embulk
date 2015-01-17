require 'optparse'

define_singleton_method(:usage) do |message|
  puts "usage: <command> [--options]"
  puts "commands:"
  puts "   bundle  [--options] [new directory]"
  puts "   run     [--options] <config.yml>"
  puts "   preview [--options] <config.yml>"
  puts "   guess   [--options] <partial-config.yml>  | tee config.yml"
  puts ""
  if message
    puts "error: #{message}"
  else
    puts "Use \`<command> --help\` to see description of the commands."
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
  #op.on('-G', '--with-guess', TrueClass) do |b|
  #  # TODO
  #end
  #op.on('-P', '--with-preview', TrueClass) do |b|
  #  # TODO
  #end
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

require 'json'
require 'yaml'

def load_config(config_path)
  java_import 'org.embulk.config.ConfigLoader'
  Embulk::Java::Injector.getInstance(ConfigLoader.java_class).fromYamlFile(java.io.File.new(config_path))
end

def setup_bundler(path)
  return unless path
  ENV['GEM_HOME'] = File.expand_path "#{path}/#{Gem.ruby_engine}/#{RbConfig::CONFIG['ruby_version']}"
  ENV['GEM_PATH'] = ''
  ENV.delete 'BUNDLE_GEMFILE'
  require 'rubygems'
  require 'bundler'
  Bundler.load.setup_environment
end

case subcmd.to_sym
when :bundle
  path = ARGV[0]
  ENV['GEM_HOME'] = File.expand_path "#{path}/#{Gem.ruby_engine}/#{RbConfig::CONFIG['ruby_version']}"
  ENV['GEM_PATH'] = ''
  ENV.delete 'BUNDLE_GEMFILE'
  if path
    tmpl = File.expand_path("#{File.dirname(__FILE__)}/../../../templates/bundle")
    require 'fileutils'
    if File.exists?(path)
      raise "Path #{path} already exists"
    end
    puts "Initializing #{path}..."
    FileUtils.mkdir_p File.dirname(path)
    FileUtils.cp_r tmpl, path
    require 'rubygems/gem_runner'
    Gem::GemRunner.new.run %w[install bundler]
    Dir.chdir(path)
  end
  require 'rubygems'
  require 'bundler'
  require 'bundler/friendly_errors'
  require 'bundler/cli'
  Bundler.with_friendly_errors do
    Bundler::CLI.start(%[install], debug: true)
  end

when :run
  config_path = ARGV[0]
  setup_bundler(conf[:bundle_path])

  java_import 'org.embulk.exec.LocalExecutor'
  java_import 'org.embulk.spi.ExecSession'

  config = load_config(config_path)
  local = Embulk::Java::Injector.getInstance(LocalExecutor.java_class)
  exec = ExecSession.new(Embulk::Java::Injector)
  result = local.run(exec, config)

  next_config = JSON.parse(result.getNextConfig().toString)
  puts YAML.dump(next_config)

when :preview
  config_path = ARGV[0]
  setup_bundler(conf[:bundle_path])

  java_import 'org.embulk.exec.PreviewExecutor'
  java_import 'org.embulk.config.ModelManager'
  java_import 'org.embulk.spi.ExecSession'
  java_import 'org.embulk.spi.Pages'
  require 'embulk/command/table_printer'

  config = load_config(config_path)
  preview = Embulk::Java::Injector.getInstance(PreviewExecutor.java_class)
  exec = ExecSession.new(Embulk::Java::Injector)
  previewed = preview.preview(exec, config)
  records = Pages.toObjects(previewed.getSchema, previewed.getPages)
  json = Embulk::Java::Injector.getInstance(ModelManager.java_class).writeObject(records)

  column_names = previewed.getSchema.getColumns.map {|c| c.getName }
  printer = TablePrinter.new(column_names)
  JSON.parse(json).each {|record|
    printer.add(record)
  }
  printer.complete

when :guess
  config_path = ARGV[0]
  setup_bundler(conf[:bundle_path])

  java_import 'org.embulk.exec.GuessExecutor'
  java_import 'org.embulk.spi.ExecSession'

  config = load_config(config_path)
  guess = Embulk::Java::Injector.getInstance(GuessExecutor.java_class)
  exec = ExecSession.new(Embulk::Java::Injector)
  guessed = guess.guess(exec, config)
  json = config.merge(guessed).toString

  puts YAML.dump(JSON.parse(json))
end
