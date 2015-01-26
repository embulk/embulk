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
      ENV['GEM_HOME'] = File.expand_path File.join(ENV['HOME'], '.embulk', Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
      Gem.clear_paths  # force rubygems to reload GEM_HOME
    end

    i = argv.find_index {|arg| arg !~ /^\-/ }
    usage nil unless i
    subcmd = argv.slice!(i)

    load_paths = []
    options = {}

    # to make sure org.embulk.jruby.JRubyScriptingModule can require 'embulk/java/bootstrap'
    $LOAD_PATH << Embulk.home('lib')

    require 'optparse'
    op = OptionParser.new

    op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
      # only for help message. implemented at lib/embulk/command/embulk.rb
    end

    case subcmd.to_sym
    when :bundle
      if default_bundle_path
        op.banner = "Usage: bundle [directory=#{default_bundle_path}]"
        args = 0..1
      else
        op.banner = "Usage: bundle <directory>"
        args = 1..1
      end

    when :run
      op.banner = "Usage: run <config.yml>"
      op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path or jar file path') do |load_path|
        load_paths << load_path
      end
      op.on('-o', '--output PATH', 'Path to a file to write the next configuration') do |path|
        options[:nextConfigOutputPath] = path
      end
      args = 1..1

    when :preview
      op.banner = "Usage: preview <config.yml>"
      op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory') do |path|
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path or jar file path') do |load_path|
        load_paths << load_path
      end
      args = 1..1

    when :guess
      op.banner = "Usage: guess <partial-config.yml>"
      op.on('-o', '--output PATH', 'Path to a file to write the guessed configuration') do |path|
        options[:nextConfigOutputPath] = path
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path or jar file path') do |load_path|
        load_paths << load_path
      end
      args = 1..1

    #when :generate  # or :new
      # TODO create plugin templates

    when :gem
      require 'rubygems/gem_runner'
      Gem::GemRunner.new.run argv
      exit 0

    when :exec
      exec *argv
      exit 127

    else
      usage "Unknown subcommand #{subcmd.dump}."
    end

    begin
      op.parse!(argv)
      unless args.include?(argv.length)
        usage nil
      end
    rescue => e
      usage e.to_s
    end

    case subcmd.to_sym
    when :bundle
      path = argv[0] || default_bundle_path

      require 'fileutils'
      require 'rubygems/gem_runner'
      setup_load_paths(load_paths)

      unless File.exists?(path)
        puts "Initializing #{path}..."
        FileUtils.mkdir_p File.dirname(path)
        begin
          success = false

          # copy embulk/data/bundle/ directory
          if __FILE__ =~ /^classpath:/ || __FILE__.include?('!/')
            # data is in embulk-core jar
            resource_class = org.embulk.command.Runner.java_class
            %w[.bundle/config embulk/input_example.rb embulk/output_example.rb examples/mydata-csv-stdout.yml examples/sample.csv.gz Gemfile Gemfile.lock].each do |file|  # TODO get file list from the jar
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

      org.embulk.command.Runner.new(options.to_json).main(subcmd, argv.to_java(:string))
    end
  end

  def self.home(dir)
    home = File.expand_path('../../..', File.dirname(__FILE__))
    File.join(home, dir)
  end

  private

  def self.setup_gem_paths(path)
    # install bundler gem here & use bundler installed here
    ENV['GEM_HOME'] = File.expand_path File.join(path, Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
    ENV['GEM_PATH'] = ''
    Gem.clear_paths  # force rubygems to reload GEM_HOME
  end

  def self.setup_load_paths(load_paths)
    load_paths.each do |load_path|
      if File.file?(load_path)
        # jar files
        require File.expand_path(load_path)
      else
        # ruby script directory (add at the beginning of $LOAD_PATH to make it highest priority)
        $LOAD_PATH.unshift File.expand_path(load_path)
      end
    end
  end

  def self.usage(message)
    STDERR.puts "usage: <command> [--options]"
    STDERR.puts "commands:"
    STDERR.puts "   bundle    [directory]                              # create or update plugin environment."
    STDERR.puts "   run       <config.yml>                             # run a bulk load transaction."
    STDERR.puts "   preview   <config.yml>                             # dry-run the bulk load without output and show preview."
    STDERR.puts "   guess     <partial-config.yml> -o <output.yml>     # guess missing parameters to create a complete configuration file."
    STDERR.puts "   gem       <install | list | help>                  # install a plugin or show installed plugins."
    STDERR.puts "                                                      # plugin path is #{ENV['GEM_HOME']}"
    STDERR.puts ""
    if message
      STDERR.puts "error: #{message}"
    else
      STDERR.puts "Use \`<command> --help\` to see description of the commands."
    end
    exit 1
  end
end
