package org.embulk;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.deps.config.YamlProcessor;
import org.embulk.deps.preview.PreviewPrinter;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ResumeState;
import org.embulk.exec.TransactionStage;
import org.embulk.jruby.LazyScriptingContainerDelegate;
import org.embulk.jruby.ScriptingContainerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EmbulkRunner runs the guess, preview, or run subcommand.
 *
 * NOTE: Developers should not depend on this EmbulkRunner class. This class is created tentatively, and may be
 * re-implemented again in a different style.
 */
public class EmbulkRunner {
    public EmbulkRunner(final EmbulkEmbed embed, final EmbulkSystemProperties embulkSystemProperties) {
        this.embed = embed;  // org.embulk.EmbulkEmbed
        this.embulkSystemProperties = embulkSystemProperties;
    }

    /**
     * Runs the guess subcommand.
     *
     * It receives Java Paths to be called from org.embulk.cli.EmbulkRun.
     */
    public void guess(final Path configFilePath, final Path outputPath) {
        // TODO: Utilize |templateParams| and |templateIncludePath|.
        // They have not been used in org.embulk.cli while |template_params| and |template_include_path| are implemented
        // in Ruby Embulk::EmbulkRunner.
        final ConfigSource configSource;
        try {
            configSource = readConfig(configFilePath, Collections.<String, Object>emptyMap(), null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try {
            guessInternal(configSource, outputPath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Runs the guess subcommand.
     *
     * It receives Strings as parameters to be called from Ruby (embulk/runner.rb).
     */
    public void guess(final String configFilePathString, final String outputPathString) {
        final Path outputPath = (outputPathString == null ? null : Paths.get(outputPathString));
        guess(Paths.get(configFilePathString), outputPath);
    }

    /**
     * Runs the guess subcommand.
     *
     * It receives a ConfigSource and a String as parameters to be called from Ruby (embulk/runner.rb).
     */
    public void guess(final ConfigSource configSource, final String outputPathString) {
        final Path outputPath = (outputPathString == null ? null : Paths.get(outputPathString));
        try {
            guessInternal(configSource, outputPath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Runs the guess subcommand.
     *
     * It receives a Java Path and a String to be called from org.embulk.cli.EmbulkRun.
     */
    public void preview(final Path configFilePath, final String format) {
        // TODO: Utilize |templateParams| and |templateIncludePath|.
        // They have not been used in org.embulk.cli while |template_params| and |template_include_path| are implemented
        // in Ruby Embulk::EmbulkRunner.
        final ConfigSource configSource;
        try {
            configSource = readConfig(configFilePath, Collections.<String, Object>emptyMap(), null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try {
            previewInternal(configSource, format);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Runs the preview subcommand.
     *
     * It receives Strings as parameters to be called from Ruby (embulk/runner.rb).
     */
    public void preview(final String configFilePathString, final String format) {
        preview(Paths.get(configFilePathString), format);
    }

    /**
     * Runs the preview subcommand.
     *
     * It receives a ConfigSource and a String as parameters to be called from Ruby (embulk/runner.rb).
     */
    public void preview(final ConfigSource configSource, final String format) {
        try {
            previewInternal(configSource, format);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Runs the run subcommand.
     *
     * It receives Java Paths to be called from org.embulk.cli.EmbulkRun.
     */
    public void run(
            final Path configFilePath,
            final Path configDiffPath,
            final Path outputPath,
            final Path resumeStatePath) {
        // TODO: Utilize |templateParams| and |templateIncludePath|.
        // They have not been used in org.embulk.cli while |template_params| and |template_include_path| are implemented
        // in Ruby Embulk::EmbulkRunner.
        final ConfigSource configSource;
        try {
            configSource = readConfig(configFilePath, Collections.<String, Object>emptyMap(), null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try {
            runInternal(configSource, configDiffPath, outputPath, resumeStatePath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Runs the run subcommand.
     *
     * It receives Strings as parameters to be called from Ruby (embulk/runner.rb).
     */
    public void run(final String configFilePathString,
                    final String configDiffPathString,
                    final String outputPathString,
                    final String resumeStatePathString) {
        final Path configDiffPath = (configDiffPathString == null ? null : Paths.get(configDiffPathString));
        final Path outputPath = (outputPathString == null ? null : Paths.get(outputPathString));
        final Path resumeStatePath = (resumeStatePathString == null ? null : Paths.get(resumeStatePathString));
        run(Paths.get(configFilePathString), configDiffPath, outputPath, resumeStatePath);
    }

    /**
     * Runs the run subcommand.
     *
     * It receives a ConfigSource and a String as parameters to be called from Ruby (embulk/runner.rb).
     */
    public void run(final ConfigSource configSource,
                    final String configDiffPathString,
                    final String outputPathString,
                    final String resumeStatePathString) {
        final Path configDiffPath = (configDiffPathString == null ? null : Paths.get(configDiffPathString));
        final Path outputPath = (outputPathString == null ? null : Paths.get(outputPathString));
        final Path resumeStatePath = (resumeStatePathString == null ? null : Paths.get(resumeStatePathString));
        try {
            runInternal(configSource, configDiffPath, outputPath, resumeStatePath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void guessInternal(final ConfigSource configSource, final Path outputPath) throws IOException {
        try {
            checkFileWritable(outputPath);
        } catch (IOException ex) {
            throw new RuntimeException("Not writable: " + outputPath.toString());
        }

        final ConfigDiff configDiff = this.embed.guess(configSource);
        final ConfigSource guessedConfigSource = configSource.merge(configDiff);
        final String yaml = writeConfig(outputPath, guessedConfigSource);
        System.err.println(yaml);
        if (outputPath != null) {
            System.out.println("Created '" + outputPath + "' file.");
        } else {
            System.out.println("Use -o PATH option to write the guessed config file to a file.");
        }
    }

    private void previewInternal(final ConfigSource configSource, final String format) throws IOException {
        final PreviewResult previewResult = this.embed.preview(configSource);

        final PreviewPrinter printer;
        switch (format != null ? format : "table") {
            case "table":
                printer = PreviewPrinter.ofTable(System.out, previewResult.getSchema());
                break;
            case "vertical":
                printer = PreviewPrinter.ofVertical(System.out, previewResult.getSchema());
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown preview output format '" + format + "'. Supported formats: table, vertical");
        }

        printer.printAllPages(previewResult.getPages());
        printer.finish();
    }

    private void runInternal(
            final ConfigSource originalConfigSource,
            final Path configDiffPath,
            final Path outputPath,  // deprecated
            final Path resumeStatePath) throws IOException {
        try {
            checkFileWritable(outputPath);
        } catch (IOException ex) {
            throw new RuntimeException("Not writable: " + outputPath.toString());
        }
        try {
            checkFileWritable(configDiffPath);
        } catch (IOException ex) {
            throw new RuntimeException("Not writable: " + configDiffPath.toString());
        }
        try {
            checkFileWritable(resumeStatePath);
        } catch (IOException ex) {
            throw new RuntimeException("Not writable: " + resumeStatePath.toString());
        }

        final ConfigSource configSource;
        if (configDiffPath != null && Files.size(configDiffPath) > 0L) {
            configSource = originalConfigSource.merge(
                    readConfig(configDiffPath, Collections.<String, Object>emptyMap(), null));
        } else {
            configSource = originalConfigSource;
        }

        final ConfigSource resumeConfig;
        if (resumeStatePath != null) {
            ConfigSource resumeConfigTemp = null;
            try {
                resumeConfigTemp = readYamlConfigFile(resumeStatePath);
            } catch (Throwable ex) {
                // TODO log?
                resumeConfigTemp = null;
            }
            if (resumeConfigTemp == null || resumeConfigTemp.isEmpty()) {
                resumeConfig = null;
            } else {
                resumeConfig = resumeConfigTemp;
            }
        } else {
            resumeConfig = null;
        }

        final EmbulkEmbed.ResumableResult resumableResult;
        final ExecutionResult executionResultTemp;
        if (resumeConfig != null) {
            resumableResult = this.embed.resumeState(configSource, resumeConfig).resume();
            executionResultTemp = null;
        } else if (resumeStatePath != null) {
            resumableResult = this.embed.runResumable(configSource);
            executionResultTemp = null;
        } else {
            resumableResult = null;
            executionResultTemp = this.embed.run(configSource);
        }

        final ExecutionResult executionResult;
        if (executionResultTemp == null) {
            if (!resumableResult.isSuccessful()) {
                if (resumableResult.getTransactionStage().isBefore(TransactionStage.RUN)) {
                    // retry without resume state file if no tasks started yet
                    // delete resume file
                    if (resumeStatePath != null) {
                        try {
                            Files.deleteIfExists(resumeStatePath);
                        } catch (Throwable ex) {
                            System.err.println("Failed to delete: " + resumeStatePath.toString());
                        }
                    }
                } else {
                    rootLogger.info("Writing resume state to '" + resumeStatePath.toString() + "'");
                    try {
                        writeResumeState(resumeStatePath, resumableResult.getResumeState());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    rootLogger.info("Resume state is written. Run the transaction again with -r option to resume or use \"cleanup\" subcommand to delete intermediate data.");
                }
                throw new RuntimeException(resumableResult.getCause());
            }
            executionResult = resumableResult.getSuccessfulResult();
        } else {
            executionResult = executionResultTemp;
        }

        // delete resume file
        if (resumeStatePath != null) {
            try {
                Files.deleteIfExists(resumeStatePath);
            } catch (Throwable ex) {
                System.err.println("Failed to delete: " + resumeStatePath.toString());
            }
        }

        final ConfigDiff configDiff = executionResult.getConfigDiff();
        rootLogger.info("Committed.");
        rootLogger.info("Input Reports: " + executionResult.getInputTaskReports());
        rootLogger.info("Output Reports: " + executionResult.getOutputTaskReports());
        rootLogger.info("Next config diff: " + configDiff.toString());

        writeConfig(configDiffPath, configDiff);
        writeConfig(outputPath, configSource.merge(configDiff));  // deprecated
    }

    // def resume_state(config, options={})
    //   configSource = read_config(config, options)
    //   Resumed.new(self, DataSource.from_java(configSource), options)
    // end

    private ConfigSource readConfig(
            final Path configFilePath,
            final Map<String, Object> templateParams,
            final String templateIncludePath) throws IOException {
        final String configString = configFilePath.toString();
        if (EXT_YAML_LIQUID.matcher(configFilePath.toString()).matches()) {
            return this.embed.newConfigLoader().fromYamlString(
                    runLiquid(new String(Files.readAllBytes(configFilePath), StandardCharsets.UTF_8),
                            templateParams,
                            (templateIncludePath == null
                                    ? configFilePath.toAbsolutePath().getParent().toString()
                                    : templateIncludePath)));
        } else if (EXT_YAML.matcher(configFilePath.toString()).matches()) {
            return this.embed.newConfigLoader().fromYamlString(
                    new String(Files.readAllBytes(configFilePath), StandardCharsets.UTF_8));
        } else {
            throw new ConfigException(
                    "Unsupported file extension. Supported file extensions are .yml and .yml.liquid: "
                    + configFilePath.toString());
        }
    }

    private ConfigSource readYamlConfigFile(final Path path) throws IOException {
        return this.embed.newConfigLoader().fromYamlString(
                new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    @SuppressWarnings("checkstyle:LineLength")
    private String runLiquid(
            final String templateSource,
            final Map<String, Object> templateParams,
            final String templateIncludePath)
            throws IOException {
        // TODO: Check if it is required to process JRuby options.
        final ScriptingContainerDelegate localJRubyContainer =
                LazyScriptingContainerDelegate.withGems(rootLogger, this.embulkSystemProperties);

        if (localJRubyContainer == null) {
            // TODO: Handle the exception better and have a better error message.
            throw new IOException("JRuby is not configured well to run Liquid. Configure the Embulk system property \"jruby\".");
        }

        localJRubyContainer.runScriptlet("require 'liquid'");

        localJRubyContainer.put("__internal_liquid_template_source__", templateSource);
        localJRubyContainer.runScriptlet("template = Liquid::Template.parse(__internal_liquid_template_source__, :error_mode => :strict)");
        localJRubyContainer.remove("__internal_liquid_template_source__");

        if (templateIncludePath != null) {
            localJRubyContainer.put("__internal_liquid_template_include_path_java__", templateIncludePath);
            localJRubyContainer.runScriptlet("__internal_liquid_template_include_path__ = File.expand_path(__internal_liquid_template_include_path_java__ || File.dirname(config)) unless __internal_liquid_template_include_path_java__ == false");
            localJRubyContainer.runScriptlet("template.registers[:file_system] = Liquid::LocalFileSystem.new(__internal_liquid_template_include_path__, \"_%s.yml.liquid\")");
            localJRubyContainer.remove("__internal_liquid_template_include_path__");
        }

        // TODO: Convert |templateParams| recursively to Ruby's Hash.
        localJRubyContainer.put("__internal_liquid_template_params__", templateParams);
        localJRubyContainer.runScriptlet("__internal_liquid_template_data__ = { 'env' => ENV.to_h }.merge(__internal_liquid_template_params__)");
        localJRubyContainer.remove("__internal_liquid_template_params__");

        final Object renderedObject =
                localJRubyContainer.runScriptlet("template.render(__internal_liquid_template_data__)");
        return renderedObject.toString();
    }

    private boolean checkFileWritable(final Path path) throws IOException {
        if (path != null) {
            // Open file with append mode and do nothing.
            // If file is not writable, it throws an exception.
            // NOTE: |Files.isWritable| does not work for the purpose as it expects the file exists.
            // Using |Files.newOutputStream| for the binary mode.
            try (final OutputStream output = Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
                ;
            }
        }
        return true;
    }

    private String writeConfig(final Path path, final DataSource modelObject) throws IOException {
        final String yamlString = dumpDataSourceInYaml(modelObject);
        if (path != null) {
            Files.write(path, yamlString.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return yamlString;
    }

    private String writeResumeState(final Path path, final ResumeState modelObject)
            throws IOException {
        final String yamlString = dumpResumeStateInYaml(modelObject);
        if (path != null) {
            Files.write(path, yamlString.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return yamlString;
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private String dumpDataSourceInYaml(final DataSource modelObject) {
        final Object object = this.embed.dumpObjectFromDataSource(modelObject);
        final YamlProcessor yamlProc = YamlProcessor.create(false);
        return yamlProc.dump(object);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private String dumpResumeStateInYaml(final ResumeState modelObject) {
        final Object object = this.embed.dumpObjectFromResumeState(modelObject);
        final YamlProcessor yamlProc = YamlProcessor.create(false);
        return yamlProc.dump(object);
    }

    // class Runnable
    //   def initialize(runner, config, options)
    //     @runner = runner
    //     @config = config
    //     @options = options
    //   end
    //
    //   attr_reader :config
    //
    //   def preview(options={})
    //     @runner.preview(@config, @options.merge(options))
    //   end
    //
    //   def run(options={})
    //     @runner.run(@config, @options.merge(options))
    //   end
    // end

    private static final Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private static final Pattern EXT_YAML = Pattern.compile(".*\\.ya?ml$");
    private static final Pattern EXT_YAML_LIQUID = Pattern.compile(".*\\.ya?ml\\.liquid$");

    private final EmbulkEmbed embed;
    private final EmbulkSystemProperties embulkSystemProperties;
}
