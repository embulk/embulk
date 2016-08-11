require 'embulk'

module Embulk
  def self.run(argv)
    # reset context class loader set by org.jruby.Main.main to nil. embulk manages
    # multiple classloaders. default classloader should be Plugin.class.getClassloader().
    java.lang.Thread.current_thread.set_context_class_loader(nil)

    require 'embulk/version'

    i = argv.find_index {|arg| arg !~ /^\-/ }
    unless i
      if argv.include?('--version')
        puts "embulk #{Embulk::VERSION}"
        system_exit_success
      end
      usage nil
    end
    subcmd = argv.slice!(i).to_sym

    require 'java'
    require 'optparse'
    op = OptionParser.new
    op.version = Embulk::VERSION

    puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: Embulk v#{Embulk::VERSION}"

    plugin_paths = []
    load_paths = []
    classpaths = []
    classpath_separator = java.io.File.pathSeparator

    options = {
      system_config: {}
    }

    java_embed_ops = lambda do
      op.separator ""
      op.separator "  Other options:"
      op.on('-l', '--log PATH', 'Output log messages to a file (default: -)') do |path|
        options[:system_config][:log_path] = path
      end
      op.on('-l', '--log-level LEVEL', 'Log level (error, warn, info, debug or trace)') do |level|
        options[:system_config][:log_level] = level
      end
      op.on('-X KEY=VALUE', 'Add a performance system config') do |kv|
        k, v = kv.split('=', 2)
        v ||= "true"
        options[:system_config][k] = v
      end
    end

    plugin_load_ops = lambda do
      op.separator ""
      op.separator "  Plugin load options:"
      op.on('-L', '--load PATH', 'Add a local plugin path') do |plugin_path|
        plugin_paths << plugin_path
      end
      op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        load_paths << load_path
      end
      op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        classpaths.concat classpath.split(classpath_separator)
      end
      op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory (create one using "embulk mkbundle" command)') do |path|
        # only for help message. implemented at lib/embulk/command/embulk_bundle.rb
      end
    end

    case subcmd
    when :run
      op.banner = "Usage: embulk run <config.yml>"
      op.separator "  Options:"
      op.on('-r', '--resume-state PATH', 'Path to a file to write or read resume state') do |path|
        options[:resume_state_path] = path
      end
      op.on('-o', '--output PATH', '(deprecated)') do |path|
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: Run with -o option is deprecated. Please use -c option instead. For example,"
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}:   $ embulk run config.yml -c diff.yml"
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: This -c option stores only diff of the next configuration."
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: The diff will be merged to the original config.yml file."
        STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
        options[:next_config_output_path] = path
      end
      op.on('-c', '--config-diff PATH', 'Path to a file to read & write the next configuration diff') do |path|
        options[:next_config_diff_path] = path
      end
      plugin_load_ops.call
      java_embed_ops.call
      args = 1..1

    when :cleanup
      op.banner = "Usage: embulk cleanup <config.yml>"
      op.separator "  Options:"
      op.on('-r', '--resume-state PATH', 'Path to a file to cleanup resume state') do |path|
        options[:resume_state_path] = path
      end
      plugin_load_ops.call
      java_embed_ops.call
      args = 1..1

    when :preview
      op.banner = "Usage: embulk preview <config.yml>"
      op.separator "  Options:"
      op.on('-G', '--vertical', "Use vertical output format", TrueClass) do |b|
        options[:format] = "vertical"
      end
      plugin_load_ops.call
      java_embed_ops.call
      args = 1..1

    when :guess
      op.banner = "Usage: embulk guess <partial-config.yml>"
      op.separator "  Options:"
      op.on('-o', '--output PATH', 'Path to a file to write the guessed configuration') do |path|
        options[:next_config_output_path] = path
      end
      op.on('-g', '--guess NAMES', "Comma-separated list of guess plugin names") do |names|
        (options[:system_config][:guess_plugins] ||= []).concat names.split(",")  # TODO
      end
      plugin_load_ops.call
      java_embed_ops.call
      args = 1..1

    when :mkbundle
      op.banner = "Usage: embulk mkbundle <directory> [--path PATH]"
      op.separator "  Options:"
      op.on('--path PATH', 'Relative path from <directory> for the location to install gems to (e.g. --path shared/bundle).') do |path|
        options[:bundle_path] = path
      end
      op.separator <<-EOF

  "mkbundle" creates a new a plugin bundle directory. You can install
  plugins (gems) to the directory instead of ~/.embulk.

  See generated <directory>/Gemfile to install plugins to the directory.
  Use -b, --bundle BUNDLE_DIR option to use it:

    $ embulk mkbundle ./dir                # create bundle directory
    $ (cd dir && vi Gemfile && embulk bundle)   # update plugin list
    $ embulk guess -b ./dir ...            # guess using bundled plugins
    $ embulk run   -b ./dir ...            # run using bundled plugins

      EOF
      args = 1..1

    when :bundle
      if argv[0] == 'new'
        usage nil if argv.length != 2
        new_bundle(argv[1], nil)
        STDERR.puts "'embulk bundle new' is deprecated. This will be removed in future release. Please use 'embulk mkbundle' instead."
      else
        run_bundler(argv)
      end
      system_exit_success

    when :gem
      require 'rubygems/gem_runner'
      Gem::GemRunner.new.run argv
      system_exit_success

    when :new
      op.banner = "Usage: embulk new <category> <name>" + %[
categories:
    ruby-input                 Ruby record input plugin    (like "mysql")
    ruby-output                Ruby record output plugin   (like "mysql")
    ruby-filter                Ruby record filter plugin   (like "add-hostname")
    #ruby-file-input           Ruby file input plugin      (like "ftp")          # not implemented yet [#21]
    #ruby-file-output          Ruby file output plugin     (like "ftp")          # not implemented yet [#22]
    ruby-parser                Ruby file parser plugin     (like "csv")
    ruby-formatter             Ruby file formatter plugin  (like "csv")
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

    when :migrate
      op.banner = "Usage: embulk migrate <directory>"
      args = 1..1

    when :selfupdate
      op.on('-f', "Skip corruption check", TrueClass) do |b|
        options[:force] = true
      end
      args = 0..1

    when :example
      args = 0..1

    when :exec
      exec(*argv)
      exit 127

    when :irb
      require 'irb'
      IRB.start
      system_exit_success

    else
      usage "Unknown subcommand #{subcmd.to_s.dump}."
    end

    begin
      op.parse!(argv)
      unless args.include?(argv.length)
        usage_op op, nil
      end
    rescue => e
      usage_op op, e.to_s
    end

    case subcmd
    when :example
      require 'embulk/command/embulk_example'
      path = ARGV[0] || "embulk-example"
      puts "Creating #{path} directory..."
      Embulk.create_example(path)
      puts ""
      puts "Run following subcommands to try embulk:"
      puts ""
      puts "   1. embulk guess #{File.join(path, 'seed.yml')} -o config.yml"
      puts "   2. embulk preview config.yml"
      puts "   3. embulk run config.yml"
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

    when :migrate
      path = ARGV[0]
      require 'embulk/command/embulk_migrate_plugin'
      Embulk.migrate_plugin(path)

    when :selfupdate
      require 'embulk/command/embulk_selfupdate'
      options[:version] = ARGV[0]
      Embulk.selfupdate(options)

    when :mkbundle
      new_bundle(argv[0], options[:bundle_path])

    else
      require 'json'

      # Gem::StubSpecification is an internal API that seems chainging often.
      # Gem::Specification.add_spec is deprecated also. Therefore, here makes
      # -L <path> option alias of -I <path>/lib by assuming that *.gemspec file
      # always has require_paths = ["lib"].
      load_paths = load_paths + plugin_paths.map {|path| File.join(path, "lib") }

      setup_load_paths(load_paths)
      setup_classpaths(classpaths)

      # call setup after setup_classpaths to allow users to overwrite
      # embulk classes
      Embulk.setup(options.delete(:system_config))

      begin
        case subcmd
        when :guess
          Embulk::Runner.guess(argv[0], options)
        when :preview
          Embulk::Runner.preview(argv[0], options)
        when :run
          Embulk::Runner.run(argv[0], options)
        end
      rescue => ex
        print_exception(ex)
        puts ""
        puts "Error: #{ex}"
        raise SystemExit.new(1, ex.to_s)
      end
    end
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

  def self.setup_load_paths(load_paths)
    # first $LOAD_PATH has highet priority. later load_paths should have highest priority.
    load_paths.each do |load_path|
      # ruby script directory (use unshift to make it highest priority)
      $LOAD_PATH.unshift File.expand_path(load_path)
    end
  end

  def self.setup_classpaths(classpaths)
    classpaths.each do |classpath|
      $CLASSPATH << classpath  # $CLASSPATH object doesn't have concat method
    end
  end

  def self.new_bundle(path, bundle_path)
    require 'fileutils'
    require 'rubygems/gem_runner'

    if File.exists?(path)
      error = "'#{path}' already exists."
      STDERR.puts error
      raise SystemExit.new(1, error)
    end

    puts "Initializing #{path}..."
    FileUtils.mkdir_p path
    begin
      success = false

      # copy embulk/data/bundle/ contents
      require 'embulk/data/package_data'
      pkg = PackageData.new("bundle", path)
      %w[Gemfile .ruby-version .bundle/config embulk/input/example.rb embulk/output/example.rb embulk/filter/example.rb].each do |file|
        pkg.cp(file, file)
      end

      # run the first bundle-install
      Dir.chdir(path) do
        run_bundler(['install', '--path', bundle_path || "."])
      end

      success = true
    rescue Gem::SystemExitException => e
      raise e if e.exit_code != 0
      success = true
    ensure
      FileUtils.rm_rf path unless success
    end
  end

  def self.run_bundler(argv)
    require 'bundler'  # bundler is included in embulk-core.jar

    # this hack is necessary to make --help working
    Bundler.define_singleton_method(:which_orig, Bundler.method(:which))
    Bundler.define_singleton_method(:which) do |executable|
      if executable == "man"
        false
      else
        which_orig(executable)
      end
    end

    require 'bundler/friendly_errors'
    require 'bundler/cli'
    Bundler.with_friendly_errors do
      Bundler::CLI.start(argv, debug: true)
    end
  end

  def self.usage(message)
    STDERR.puts "Embulk v#{Embulk::VERSION}"
    STDERR.puts "Usage: embulk [-vm-options] <command> [--options]"
    STDERR.puts "Commands:"
    STDERR.puts "   mkbundle   <directory>                             # create a new plugin bundle environment."
    STDERR.puts "   bundle     [directory]                             # update a plugin bundle environment."
    STDERR.puts "   run        <config.yml>                            # run a bulk load transaction."
    STDERR.puts "   cleanup    <config.yml>                            # cleanup resume state."
    STDERR.puts "   preview    <config.yml>                            # dry-run the bulk load without output and show preview."
    STDERR.puts "   guess      <partial-config.yml> -o <output.yml>    # guess missing parameters to create a complete configuration file."
    STDERR.puts "   gem        <install | list | help>                 # install a plugin or show installed plugins."
    STDERR.puts "                                                      # plugin path is #{ENV['GEM_HOME']}"
    STDERR.puts "   new        <category> <name>                       # generates new plugin template"
    STDERR.puts "   migrate    <path>                                  # modify plugin code to use the latest Embulk plugin API"
    STDERR.puts "   example    [path]                                  # creates an example config file and csv file to try embulk."
    STDERR.puts "   selfupdate [version]                               # upgrades embulk to the latest released version or to the specified version."
    STDERR.puts ""
    STDERR.puts "VM options:"
    STDERR.puts "   -J-O                             Disable JVM optimizations to speed up startup time (enabled by default if command is 'run')"
    STDERR.puts "   -J+O                             Enable JVM optimizations to speed up throughput"
    STDERR.puts "   -J...                            Set JVM options (use -J-help to see available options)"
    STDERR.puts "   -R...                            Set JRuby options (use -R--help to see available options)"
    STDERR.puts ""
    if message
      system_exit "error: #{message}"
    else
      system_exit "Use \`<command> --help\` to see description of the commands."
    end
  end

  def self.usage_op(op, message)
    STDERR.puts op.help
    STDERR.puts
    system_exit message
  end

  def self.print_exception(ex)
    if ex.respond_to?(:to_java) && ex.is_a?(java.lang.Throwable)
      ex.to_java.printStackTrace(java.lang.System.out)
    else
      puts "#{ex.to_s}"
      ex.backtrace.each do |bt|
        puts "    #{bt}"
      end
    end
  end

  def self.system_exit(message=nil)
    STDERR.puts message if message
    raise SystemExit.new(1, message)
  end

  def self.system_exit_success
    raise SystemExit.new(0)
  end
end
