module Embulk

  class EmbulkRunner
    def initialize(runner_java)
      @runner_java = runner_java  # Java::org.embulk.EmbulkRunner
    end

    def guess(config, options={})
      output_path =
        (options[:next_config_output_path] ? Java::java.lang.String.new(options[:next_config_output_path]) : nil)

      case config
      when String
        @runner_java.guess(Java::java.lang.String.new(config), output_path)
      when Hash, DataSource
        @runner_java.guess(DataSource.new(config).to_java, output_path)
      end

      nil
    end

    def preview(config, options={})
      format = (options[:format] ? Java::java.lang.String.new(options[:format]) : nil)

      case config
      when String
        @runner_java.preview(Java::java.lang.String.new(config), format)
      when Hash, DataSource
        @runner_java.preview(DataSource.new(config).to_java, format)
      end

      nil
    end

    def run(config, options={})
      config_diff_path =
        (options[:next_config_diff_path] ? Java::java.lang.String.new(options[:next_config_diff_path]) : nil)
      output_path =  # deprecated
        (options[:next_config_output_path] ? Java::java.lang.String.new(options[:next_config_output_path]) : nil)
      resume_state_path =
        (options[:resume_state_path] ? Java::java.lang.String.new(options[:resume_state_path]) : nil)

      case config
      when String
        @runner_java.run(Java::java.lang.String.new(config), config_diff_path, output_path, resume_state_path)
      when Hash, DataSource
        @runner_java.run(DataSource.new(config).to_java, config_diff_path, output_path, resume_state_path)
      end

      nil
    end
  end
end
