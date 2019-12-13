package org.embulk.cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.embulk.EmbulkSystemProperties;

public class EmbulkCommandLine {
    private EmbulkCommandLine(
            final List<String> arguments,
            final Properties embulkSystemProperties,
            final String bundlePath,
            final String configDiff,
            final boolean force,
            final String format,
            final String output,
            final String resumeState) {
        this.arguments = Collections.unmodifiableList(arguments);
        this.embulkSystemProperties = EmbulkSystemProperties.of(embulkSystemProperties);
        this.bundlePath = bundlePath;
        this.configDiff = configDiff;
        this.force = force;
        this.format = format;
        this.output = output;
        this.resumeState = resumeState;
    }

    public static final class Builder {
        public Builder() {
            this.arguments = new ArrayList<String>();
            this.embulkSystemProperties = new Properties();
            this.bundlePath = null;
            this.configDiff = null;
            this.force = false;
            this.format = null;
            this.load = new ArrayList<String>();
            this.loadPath = new ArrayList<String>();
            this.output = null;
            this.resumeState = null;
        }

        public EmbulkCommandLine build() {
            final Properties embulkSystemPropertiesJRubyLoadPath = new Properties(this.embulkSystemProperties);

            final ArrayList<String> jrubyLoadPaths = new ArrayList<>();
            // first $LOAD_PATH has highest priority. later load_paths should have highest priority.
            for (final String oneJRubyLoadPath : this.loadPath) {  // "-L"
                jrubyLoadPaths.add(oneJRubyLoadPath);
            }
            // Gem::StubSpecification is an internal API that seems chainging often.
            // Gem::Specification.add_spec is deprecated also. Therefore, here makes
            // -L <path> option alias of -I <path>/lib by assuming that *.gemspec file
            // always has require_paths = ["lib"].
            for (final String oneJRubyLoadPathToAddLib : this.load) {  // "-I"
                jrubyLoadPaths.add(Paths.get(oneJRubyLoadPathToAddLib).resolve("lib").toString());
            }
            embulkSystemPropertiesJRubyLoadPath.setProperty(
                    "jruby_load_path", String.join(java.io.File.pathSeparator, jrubyLoadPaths));
            if (this.bundlePath == null) {
                embulkSystemPropertiesJRubyLoadPath.setProperty("jruby_use_default_embulk_gem_home", "true");
            }
            return new EmbulkCommandLine(
                    this.arguments,
                    embulkSystemPropertiesJRubyLoadPath,
                    this.bundlePath,
                    this.configDiff,
                    this.force,
                    this.format,
                    this.output,
                    this.resumeState);
        }

        public Builder addArguments(final List<String> arguments) {
            this.arguments.addAll(arguments);
            return this;
        }

        public Builder setEmbulkSystemProperties(final String key, final String value) {
            this.embulkSystemProperties.put(key, value);
            return this;
        }

        public Builder setBundlePath(final String bundlePath) {
            this.bundlePath = bundlePath;
            return this;
        }

        public Builder setConfigDiff(final String configDiff) {
            this.configDiff = configDiff;
            return this;
        }

        public Builder setForce(final boolean force) {
            this.force = force;
            return this;
        }

        public Builder setFormat(final String format) {
            this.format = format;
            return this;
        }

        public Builder addLoad(final String load) {
            this.load.add(load);
            return this;
        }

        public Builder addLoadPath(final String loadPath) {
            this.loadPath.add(loadPath);
            return this;
        }

        public Builder setOutput(final String output) {
            this.output = output;
            return this;
        }

        public Builder setResumeState(final String resumeState) {
            this.resumeState = resumeState;
            return this;
        }

        private ArrayList<String> arguments;
        private Properties embulkSystemProperties;
        private String bundlePath;
        private String configDiff;
        private boolean force;
        private String format;
        private ArrayList<String> load;
        private ArrayList<String> loadPath;
        private String output;
        private String resumeState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public final List<String> getArguments() {
        return this.arguments;
    }

    public final EmbulkSystemProperties getEmbulkSystemProperties() {
        return this.embulkSystemProperties;
    }

    public final String getBundlePath() {
        return this.bundlePath;
    }

    public final String getConfigDiff() {
        return this.configDiff;
    }

    public final boolean getForce() {
        return this.force;
    }

    public final String getFormat() {
        return this.format;
    }

    public final String getOutput() {
        return this.output;
    }

    public final String getResumeState() {
        return this.resumeState;
    }

    private final List<String> arguments;
    private final EmbulkSystemProperties embulkSystemProperties;
    private final String bundlePath;
    private final String configDiff;
    private final boolean force;
    private final String format;
    private final String output;
    private final String resumeState;
}
