package org.embulk.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbulkCommandLine
{
    private EmbulkCommandLine(
            final List<String> arguments,
            final Map<String, Object> systemConfig,
            final String bundle,
            final String bundlePath,
            final List<String> classpath,
            final String configDiff,
            final boolean force,
            final String format,
            final List<String> load,
            final List<String> loadPath,
            final String output,
            final String resumeState)
    {
        this.arguments = Collections.unmodifiableList(arguments);
        this.systemConfig = Collections.unmodifiableMap(systemConfig);
        this.bundle = bundle;
        this.bundlePath = bundlePath;
        this.classpath = Collections.unmodifiableList(classpath);
        this.configDiff = configDiff;
        this.force = force;
        this.format = format;
        this.load = Collections.unmodifiableList(load);
        this.loadPath = Collections.unmodifiableList(loadPath);
        this.output = output;
        this.resumeState = resumeState;
    }

    public static final class Builder
    {
        public Builder()
        {
            this.arguments = new ArrayList<String>();
            this.systemConfig = new HashMap<String, Object>();
            this.bundle = null;
            this.bundlePath = null;
            this.classpath = new ArrayList<String>();
            this.configDiff = null;
            this.force = false;
            this.format = null;
            this.load = new ArrayList<String>();
            this.loadPath = new ArrayList<String>();
            this.output = null;
            this.resumeState = null;
        }

        public EmbulkCommandLine build()
        {
            return new EmbulkCommandLine(
                this.arguments,
                this.systemConfig,
                this.bundle,
                this.bundlePath,
                this.classpath,
                this.configDiff,
                this.force,
                this.format,
                this.load,
                this.loadPath,
                this.output,
                this.resumeState);
        }

        public Builder addArguments(final List<String> arguments)
        {
            this.arguments.addAll(arguments);
            return this;
        }

        public Builder setSystemConfig(final String key, final String value)
        {
            this.systemConfig.put(key, value);
            return this;
        }

        public Builder addSystemConfig(final String key, final String value)
        {
            final Object existingValue = this.systemConfig.get(key);
            if (existingValue != null && existingValue instanceof String) {
                this.systemConfig.put(key, Arrays.asList((String) existingValue, value));
            }
            else if (existingValue != null && existingValue instanceof List) {
                @SuppressWarnings("unchecked")
                final ArrayList<String> newList = new ArrayList<String>((List<String>) existingValue);
                newList.add(value);
                this.systemConfig.put(key, Collections.unmodifiableList(newList));
            }
            else {
                this.systemConfig.put(key, Arrays.asList(value));
            }
            return this;
        }

        public Builder setBundle(final String bundle)
        {
            this.bundle = bundle;
            return this;
        }

        public Builder setBundlePath(final String bundlePath)
        {
            this.bundlePath = bundlePath;
            return this;
        }

        public Builder addClasspath(final String classpath)
        {
            this.classpath.add(classpath);
            return this;
        }

        public Builder setConfigDiff(final String configDiff)
        {
            this.configDiff = configDiff;
            return this;
        }

        public Builder setForce(final boolean force)
        {
            this.force = force;
            return this;
        }

        public Builder setFormat(final String format)
        {
            this.format = format;
            return this;
        }

        public Builder addLoad(final String load)
        {
            this.load.add(load);
            return this;
        }

        public Builder addLoadPath(final String loadPath)
        {
            this.loadPath.add(loadPath);
            return this;
        }

        public Builder setOutput(final String output)
        {
            this.output = output;
            return this;
        }

        public Builder setResumeState(final String resumeState)
        {
            this.resumeState = resumeState;
            return this;
        }

        private ArrayList<String> arguments;
        private HashMap<String, Object> systemConfig;
        private String bundle;
        private String bundlePath;
        private ArrayList<String> classpath;
        private String configDiff;
        private boolean force;
        private String format;
        private ArrayList<String> load;
        private ArrayList<String> loadPath;
        private String output;
        private String resumeState;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public final List<String> getArguments()
    {
        return this.arguments;
    }

    public final Map<String, Object> getSystemConfig()
    {
        return this.systemConfig;
    }

    public final String getBundle()
    {
        return this.bundle;
    }

    public final String getBundlePath()
    {
        return this.bundlePath;
    }

    public final List<String> getClasspath()
    {
        return this.classpath;
    }

    public final String getConfigDiff()
    {
        return this.configDiff;
    }

    public final boolean getForce()
    {
        return this.force;
    }

    public final String getFormat()
    {
        return this.format;
    }

    public final List<String> getLoad()
    {
        return this.load;
    }

    public final List<String> getLoadPath()
    {
        return this.loadPath;
    }

    public final String getOutput()
    {
        return this.output;
    }

    public final String getResumeState()
    {
        return this.resumeState;
    }

    private final List<String> arguments;
    private final Map<String, Object> systemConfig;
    private final String bundle;
    private final String bundlePath;
    private final List<String> classpath;
    private final String configDiff;
    private final boolean force;
    private final String format;
    private final List<String> load;
    private final List<String> loadPath;
    private final String output;
    private final String resumeState;
}
