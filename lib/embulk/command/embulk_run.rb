module Embulk
  def self.run(load_paths, subcmd, argv, options)
    case subcmd.to_sym
    when :bundle
      path = argv[0] || "."

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

      org.embulk.command.Runner.new(options.to_json).main(subcmd, argv.to_java(:string))
    end
  end

  def self.setup_load_paths(load_paths)
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

end
