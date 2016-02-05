module Embulk

  # Embulk.setup initializes:
  # Runner = EmbulkRunner.new

  class EmbulkRunner
    def initialize(embed)
      @embed = embed  # org.embulk.EmbulkEmbed
    end

    def guess(config, options={})
      configSource = read_config(config, options)
      output_path = options[:next_config_output_path]

      check_file_writable(output_path)

      configDiff = @embed.guess(configSource)

      guessedConfigSource = configSource.merge(configDiff)
      yaml = write_config(output_path, guessedConfigSource)
      STDERR.puts yaml
      if output_path
        puts "Created '#{output_path}' file."
      else
        puts "Use -o PATH option to write the guessed config file to a file."
      end

      nil
    end

    def preview(config, options={})
      configSource = read_config(config, options)
      format = options[:format]

      previewResult = @embed.preview(configSource)

      modelManager = @embed.getModelManager
      printer =
        case format || "table"
        when "table"
          org.embulk.command.TablePreviewPrinter.new(java.lang.System.out, modelManager, previewResult.getSchema)
        when "vertical"
          org.embulk.command.VerticalPreviewPrinter.new(java.lang.System.out, modelManager, previewResult.getSchema)
        else
          raise ArgumentError, "Unknown preview output format '#{format}'. Supported formats: table, vertical"
        end

      printer.printAllPages(previewResult.getPages)
      printer.finish

      nil
    end

    def run(config, options={})
      configSource = read_config(config, options)
      config_diff_path = options[:next_config_diff_path]
      output_path = options[:next_config_output_path]  # deprecated
      resume_state_path = options[:resume_state_path]

      check_file_writable(output_path)  # deprecated
      check_file_writable(config_diff_path)
      check_file_writable(resume_state_path)

      if config_diff_path && File.size(config_diff_path) > 0
        lastConfigDiff = read_config(config_diff_path, options)
        configSource = configSource.merge(lastConfigDiff)
      end

      if resume_state_path
        begin
          resumeConfig = read_yaml_config_file(resume_state_path)
          resumeConfig = nil if resumeConfig.isEmpty
        rescue
          # TODO log?
          resumeConfig = nil
        end
      end

      if resumeConfig
        resumableResult = @embed.resumeState(configSource, resumeConfig).resume
      elsif resume_state_path
        resumableResult = @embed.runResumable(configSource)
      else
        executionResult = @embed.run(configSource)
      end

      unless executionResult
        unless resumableResult.isSuccessful
          if resumableResult.getTransactionStage.isBefore(org.embulk.exec.TransactionStage::RUN)
            # retry without resume state file if no tasks started yet
            # delete resume file
            File.delete(resume_state_path) rescue nil if resume_state_path
          else
            Embulk.logger.info "Writing resume state to '#{resume_state_path}'"
            write_config(resume_state_path, resumableResult.getResumeState)
            Embulk.logger.info "Resume state is written. Run the transaction again with -r option to resume or use \"cleanup\" subcommand to delete intermediate data."
          end
          raise resumableResult.getCause
        end
        executionResult = resumableResult.getSuccessfulResult
      end

      # delete resume file
      File.delete(resume_state_path) rescue nil if resume_state_path

      configDiff = executionResult.getConfigDiff
      Embulk.logger.info("Committed.")
      Embulk.logger.info("Next config diff: #{configDiff.toString}")

      write_config(config_diff_path, configDiff)
      write_config(output_path, configSource.merge(configDiff))  # deprecated
    end

    #def resume_state(config, options={})
    #  configSource = read_config(config, options)
    #  Resumed.new(self, DataSource.from_java(configSource), options)
    #end

    private

    def read_config(config, options={})
      case config
      when String
        case config
        when /\.ya?ml\.liquid$/
          require 'liquid'
          template_params = options[:template_params] || {}
          template_include_path = File.expand_path(options[:template_include_path] || File.dirname(config)) unless options[:template_include_path] == false
          @embed.newConfigLoader.fromYamlString run_liquid(File.read(config), template_params, template_include_path)
        when /\.ya?ml$/
          @embed.newConfigLoader.fromYamlString File.read(config)
        else
          raise ConfigError.new("Unsupported file extension. Supported file extensions are .yml and .yml.liquid: #{config}")
        end

      when Hash, DataSource
        DataSource.new(config).to_java
      end
    end

    def read_yaml_config_file(path)
      @embed.newConfigLoader.fromYamlString File.read(config)
    end

    def run_liquid(source, params, template_include_path)
      require 'liquid'
      template = Liquid::Template.parse(source, :error_mode => :strict)
      template.registers[:file_system] = Liquid::LocalFileSystem.new(template_include_path, "_%s.yml.liquid") if template_include_path

      data = {
        "env" => ENV.to_h,
      }.merge(params)

      template.render(data)
    end

    def check_file_writable(path)
      if path
        # Open file with append mode and do nothing.
        # If file is not writable, it throws an exception.
        File.open(path, "ab")
      end
    end

    def write_config(path, modelObject)
      yaml = dump_yaml(modelObject)
      if path
        File.write(path, yaml)
      end
      yaml
    end

    def dump_yaml(modelObject)
      modelManager = @embed.getModelManager
      obj = modelManager.readObject(java.lang.Object.java_class, modelManager.writeObject(modelObject))
      return org.yaml.snakeyaml.Yaml.new.dump(obj)
    end

    #class Runnable
    #  def initialize(runner, config, options)
    #    @runner = runner
    #    @config = config
    #    @options = options
    #  end
    #
    #  attr_reader :config
    #
    #  def preview(options={})
    #    @runner.preview(@config, @options.merge(options))
    #  end
    #
    #  def run(options={})
    #    @runner.run(@config, @options.merge(options))
    #  end
    #end
  end

end
