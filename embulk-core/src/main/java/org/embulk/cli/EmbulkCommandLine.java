package org.embulk.cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbulkCommandLine {
    private EmbulkCommandLine(
            final List<String> arguments,
            final Map<String, Object> systemConfig,
            final String bundlePath,
            final String configDiff,
            final boolean force,
            final String format,
            final String output,
            final String resumeState) {
        this.arguments = Collections.unmodifiableList(arguments);
        this.systemConfig = Collections.unmodifiableMap(systemConfig);
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
            this.systemConfig = new HashMap<String, Object>();
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
            final HashMap<String, Object> systemConfigJRubyLoadPath = new HashMap<String, Object>(this.systemConfig);
            // first $LOAD_PATH has highest priority. later load_paths should have highest priority.
            for (final String oneJRubyLoadPath : this.loadPath) {  // "-L"
                addInHashMap(systemConfigJRubyLoadPath, "jruby_load_path", oneJRubyLoadPath);
            }
            // Gem::StubSpecification is an internal API that seems chainging often.
            // Gem::Specification.add_spec is deprecated also. Therefore, here makes
            // -L <path> option alias of -I <path>/lib by assuming that *.gemspec file
            // always has require_paths = ["lib"].
            for (final String oneJRubyLoadPathToAddLib : this.load) {  // "-I"
                addInHashMap(systemConfigJRubyLoadPath,
                             "jruby_load_path",
                             Paths.get(oneJRubyLoadPathToAddLib).resolve("lib").toString());
            }
            if (this.bundlePath == null) {
                systemConfigJRubyLoadPath.put("jruby_use_default_embulk_gem_home", "true");
            }
            return new EmbulkCommandLine(
                    this.arguments,
                    systemConfigJRubyLoadPath,
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

        public Builder setSystemConfig(final String key, final String value) {
            this.systemConfig.put(key, value);
            return this;
        }

        public Builder addSystemConfig(final String key, final String value) {
            addInHashMap(this.systemConfig, key, value);
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

        private static void addInHashMap(final HashMap<String, Object> map, final String key, final String value) {
            final Object existingValue = map.get(key);
            if (existingValue != null && existingValue instanceof String) {
                map.put(key, Arrays.asList((String) existingValue, value));
            } else if (existingValue != null && existingValue instanceof List) {
                @SuppressWarnings("unchecked")
                final ArrayList<String> newList = new ArrayList<String>((List<String>) existingValue);
                newList.add(value);
                map.put(key, Collections.unmodifiableList(newList));
            } else {
                map.put(key, Arrays.asList(value));
            }
        }

        private ArrayList<String> arguments;
        private HashMap<String, Object> systemConfig;
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

    public final Map<String, Object> getSystemConfig() {
        return this.systemConfig;
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
    private final Map<String, Object> systemConfig;
    private final String bundlePath;
    private final String configDiff;
    private final boolean force;
    private final String format;
    private final String output;
    private final String resumeState;
}
