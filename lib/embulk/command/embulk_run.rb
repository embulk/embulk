module Embulk
  def self.run(argv)
    # default_bundle_path
    default_bundle_path = nil
    gemfile_path = ENV['BUNDLE_GEMFILE'].to_s
    gemfile_path = nil if gemfile_path.empty?
    default_bundle_path = File.dirname(gemfile_path) if gemfile_path

    # default GEM_HOME is ~/.embulk/jruby/1.9/. If -b option is set,
    # GEM_HOME is already set by embulk/command/embulk.rb
    gem_home = ENV['GEM_HOME'].to_s
    if gem_home.empty?
      ENV['GEM_HOME'] = default_gem_home
      Gem.clear_paths  # force rubygems to reload GEM_HOME
    end

    # to make sure org.embulk.jruby.JRubyScriptingModule can require 'embulk/java/bootstrap'
    $LOAD_PATH << Embulk.home('lib')

    require 'embulk/version'

    i = argv.find_index {|arg| arg !~ /^\-/ }
    unless i
      if argv.include?('--version')
        puts "embulk #{Embulk::VERSION}"
        exit 0
      end
      usage nil
    end
    subcmd = argv.slice!(i)

    require 'java'
    require 'optparse'
    op = OptionParser.new
    op.version = Embulk::VERSION

    puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S,%3N %z")}: Embulk v#{Embulk::VERSION}"

    load_paths = []
    classpaths = []
    classpath_separator = java.io.File.pathSeparator
    options = {}

    op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
      # only for help message. implemented at lib/embulk/command/embulk.rb
    end

    case subcmd.to_sym
    when :bundle
      op.remove  # remove --bundle
      if default_bundle_path
        op.banner = "Usage: bundle [directory=#{default_bundle_path}]"
        args = 0..1
      else
        op.banner = "Usage: bundle <directory>"
        args = 1..1
      end

    when :run
      op.banner = "Usage: run <config.yml>"
      op.on('-l', '--log-level LEVEL', 'Log level (fatal, error, warn, info, or trace)') do |level|
        options[:logLevel] = level
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        load_paths << load_path
      end
      op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        classpaths.concat classpath.split(classpath_separator)
      end
      op.on('-o', '--output PATH', 'Path to a file to write the next configuration') do |path|
        options[:nextConfigOutputPath] = path
      end
      op.on('-r', '--resume-state PATH', 'Path to a file to write or read resume state') do |path|
        options[:resumeStatePath] = path
      end
      args = 1..1

    when :cleanup
      op.banner = "Usage: cleanup <config.yml>"
      op.on('-l', '--log-level LEVEL', 'Log level (fatal, error, warn, info, or trace)') do |level|
        options[:logLevel] = level
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        load_paths << load_path
      end
      op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        classpaths.concat classpath.split(classpath_separator)
      end
      op.on('-r', '--resume-state PATH', 'Path to a file to write or read resume state') do |path|
        options[:resumeStatePath] = path
      end
      args = 1..1

    when :preview
      op.banner = "Usage: preview <config.yml>"
      op.on('-l', '--log-level LEVEL', 'Log level (fatal, error, warn, info, or trace)') do |level|
        options[:logLevel] = level
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        load_paths << load_path
      end
      op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        classpaths.concat classpath.split(classpath_separator)
      end
      op.on('-S', '--output-style SYTLE', 'Preview output style (table or vertical)') do |style|
        options[:outputStyle] = style
      end
      args = 1..1

    when :guess
      op.banner = "Usage: guess <partial-config.yml>"
      op.on('-l', '--log-level LEVEL', 'Log level (fatal, error, warn, info, or trace)') do |level|
        options[:logLevel] = level
      end
      op.on('-o', '--output PATH', 'Path to a file to write the guessed configuration') do |path|
        options[:nextConfigOutputPath] = path
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        load_paths << load_path
      end
      op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        classpaths.concat classpath.split(classpath_separator)
      end
      args = 1..1

    when :new
      op.remove  # remove --bundle
      op.banner = "Usage: new <category> <name>" + %[
categories:
    ruby-input                 Ruby record input plugin    (like "mysql")
    ruby-output                Ruby record output plugin   (like "mysql")
    ruby-filter                Ruby record filter plugin   (like "add-hostname")
    #ruby-file-input           Ruby file input plugin      (like "ftp")          # not implemented yet [#21]
    #ruby-file-output          Ruby file output plugin     (like "ftp")          # not implemented yet [#22]
    ruby-parser                Ruby file parser plugin     (like "csv")          # not implemented yet [#33]
    ruby-formatter             Ruby file formatter plugin  (like "csv")          # not implemented yet [#34]
    #ruby-decoder              Ruby file decoder plugin    (like "gzip")         # not implemented yet [#31]
    #ruby-encoder              Ruby file encoder plugin    (like "gzip")         # not implemented yet [#32]
    java-input                 Java record input plugin    (like "mysql")
    java-output                Java record output plugin   (like "mysql")
    java-filter                Java record filter plugin   (like "add-hostname")
    java-file-input            Java file input plugin      (like "ftp")
    java-file-output           Java file output plugin     (like "ftp")
    java-parser                Java file parser plugin     (like "csv")
    java-formatter             Java file formatter plugin  (like "csv")
    java-decoder               Java file decoder plugin    (like "gzip")
    java-encoder               Java file encoder plugin    (like "gzip")

examples:
    new ruby-output hbase
    new ruby-filter int-to-string
]
      args = 2..2

    when :gem
      require 'rubygems/gem_runner'
      Gem::GemRunner.new.run argv
      exit 0

    when :example
      args = 0..1

    when :exec
      exec *argv
      exit 127

    else
      usage "Unknown subcommand #{subcmd.dump}."
    end

    begin
      op.parse!(argv)
      unless args.include?(argv.length)
        usage_op op, nil
      end
    rescue => e
      usage_op op, e.to_s
    end

    case subcmd.to_sym
    when :bundle
      path = argv[0] || default_bundle_path

      require 'fileutils'
      require 'rubygems/gem_runner'
      setup_load_paths(load_paths)
      setup_classpaths(classpaths)

      unless File.exists?(path)
        puts "Initializing #{path}..."
        FileUtils.mkdir_p File.dirname(path)
        begin
          success = false

          # copy embulk/data/bundle/ directory
          require 'embulk/data/package_data'
          pkg = PackageData.new("bundle", path)
          %w[.bundle/config embulk/input/example.rb embulk/output/example.rb embulk/filter/example.rb Gemfile].each do |file|
            pkg.cp(file, file)
          end

          ## TODO this is disabled for now. enable this if you want to use
          ## create bin/embulk
          #bin_embulk_path = File.join(path, 'bin', 'embulk')
          #FileUtils.mkdir_p File.dirname(bin_embulk_path)
          #require 'embulk/command/embulk_generate_bin'  # defines Embulk.generate_bin
          #File.open(bin_embulk_path, 'wb', 0755) {|f| f.write Embulk.generate_bin(bundle_path: :here) }

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

      ENV['BUNDLE_GEMFILE'] = File.expand_path File.join(path, "Gemfile")
      Dir.chdir(path) do
        require 'bundler'
        require 'bundler/friendly_errors'
        require 'bundler/cli'
        Bundler.with_friendly_errors do
          # run > bundle install
          Bundler::CLI.start(%w[install], debug: true)
        end
      end

    when :example
      require_relative 'embulk_example'
      path = ARGV[0] || "embulk-example"
      puts "Creating #{path} directory..."
      Embulk.create_example(path)
      puts ""
      puts "Run following subcommands to try embulk:"
      puts ""
      puts "   1. guess #{File.join(path, 'example.yml')} -o config.yml"
      puts "   2. preview config.yml"
      puts "   3. run config.yml"
      puts ""

    when :new
      lang_cate = ARGV[0]
      name = ARGV[1]

      language, category = case lang_cate
        when "java-input"       then [:java, :input]
        when "java-output"      then [:java, :output]
        when "java-filter"      then [:java, :filter]
        when "java-file-input"  then [:java, :file_input]
        when "java-file-output" then [:java, :file_output]
        when "java-parser"      then [:java, :parser]
        when "java-formatter"   then [:java, :formatter]
        when "java-decoder"     then [:java, :decoder]
        when "java-encoder"     then [:java, :encoder]
        when "ruby-input"       then [:ruby, :input]
        when "ruby-output"      then [:ruby, :output]
        when "ruby-filter"      then [:ruby, :filter]
        when "ruby-file-input"  then raise "ruby-file-input is not implemented yet. See #21 on github." #[:ruby, :file_input]
        when "ruby-file-output" then raise "ruby-file-output is not implemented yet. See #22 on github." #[:ruby, :file_output]
        when "ruby-parser"      then [:ruby, :parser]
        when "ruby-formatter"   then [:ruby, :formatter]
        when "ruby-decoder"     then raise "ruby-decoder is not implemented yet. See #31 on github." #[:ruby, :decoder]
        when "ruby-encoder"     then raise "ruby-decoder is not implemented yet. See #32 on github." #[:ruby, :encoder]
        else
          usage_op op, "Unknown category '#{lang_cate}'"
        end

      require 'embulk/command/embulk_new_plugin'
      Embulk.new_plugin(name, language, category)

    else
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
      setup_classpaths(classpaths)

      org.embulk.command.Runner.new(options.to_json).main(subcmd, argv.to_java(:string))
    end
  end

  def self.home(dir)
    home = File.expand_path('../../..', File.dirname(__FILE__))
    File.join(home, dir)
  end

  def self.default_gem_home
    if RUBY_PLATFORM =~ /java/i
      user_home = java.lang.System.properties["user.home"]
    end
    user_home ||= ENV['HOME']
    unless user_home
      raise "HOME environment variable is not set."
    end
    File.expand_path File.join(user_home, '.embulk', Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
  end

  private

  def self.setup_gem_paths(path)
    # install bundler gem here & use bundler installed here
    ENV['GEM_HOME'] = File.expand_path File.join(path, Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
    ENV['GEM_PATH'] = ''
    Gem.clear_paths  # force rubygems to reload GEM_HOME
  end

  def self.setup_load_paths(load_paths)
    # first $LOAD_PATH has highet priority. later load_paths should have highest priority.
    load_paths.each do |load_path|
      # ruby script directory (use unshift to make it highest priority)
      $LOAD_PATH.unshift File.expand_path(load_path)
    end
  end

  def self.setup_classpaths(classpaths)
    classpaths.each {|classpath|
      $CLASSPATH << classpath  # $CLASSPATH object doesn't have concat method
    }
  end

  def self.usage(message)
    STDERR.puts "Embulk v#{Embulk::VERSION}"
    STDERR.puts "usage: <command> [--options]"
    STDERR.puts "commands:"
    STDERR.puts "   bundle    [directory]                              # create or update plugin environment."
    STDERR.puts "   run       <config.yml>                             # run a bulk load transaction."
    STDERR.puts "   preview   <config.yml>                             # dry-run the bulk load without output and show preview."
    STDERR.puts "   guess     <partial-config.yml> -o <output.yml>     # guess missing parameters to create a complete configuration file."
    STDERR.puts "   gem       <install | list | help>                  # install a plugin or show installed plugins."
    STDERR.puts "                                                      # plugin path is #{ENV['GEM_HOME']}"
    STDERR.puts "   new       <category> <name>                        # generates new plugin template"
    STDERR.puts "   example   [path]                                   # creates an example config file and csv file to try embulk."
    STDERR.puts ""
    if message
      STDERR.puts "error: #{message}"
    else
      STDERR.puts "Use \`<command> --help\` to see description of the commands."
    end
    exit 1
  end

  def self.usage_op(op, message)
    STDERR.puts op.help
    STDERR.puts
    if message
      STDERR.puts message
    end
    exit 1
  end
end
