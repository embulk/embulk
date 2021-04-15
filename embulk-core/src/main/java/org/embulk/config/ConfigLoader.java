package org.embulk.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.embulk.deps.config.ConfigLoaderDelegate;

public class ConfigLoader {
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public ConfigLoader(ModelManager model) {
        this.delegate = ConfigLoaderDelegate.of(model);
    }

    public ConfigSource newConfigSource() {
        return this.delegate.newConfigSource();
    }

    public ConfigSource fromJsonString(final String string) {
        return this.delegate.fromJsonString(string);
    }

    public ConfigSource fromJsonFile(final File file) throws IOException {
        return this.delegate.fromJsonFile(file);
    }

    public ConfigSource fromJson(final InputStream stream) throws IOException {
        return this.delegate.fromJson(stream);
    }

    public ConfigSource fromYamlString(final String string) {
        return this.delegate.fromYamlString(string);
    }

    public ConfigSource fromYamlFile(final File file) throws IOException {
        return this.delegate.fromYamlFile(file);
    }

    public ConfigSource fromYaml(final InputStream stream) throws IOException {
        return this.delegate.fromYaml(stream);
    }

    // The method is removed: public ConfigSource fromJson(JsonParser parser) throws IOException

    public ConfigSource fromPropertiesYamlLiteral(final Properties props, final String keyPrefix) {
        return this.delegate.fromPropertiesYamlLiteral(props, keyPrefix);
    }

    public ConfigSource fromPropertiesYamlLiteral(final Map<String, String> props, final String keyPrefix) {
        return this.delegate.fromPropertiesYamlLiteral(props, keyPrefix);
    }

    // The method is removed: public ConfigSource fromPropertiesAsIs(final Properties properties)

    private final ConfigLoaderDelegate delegate;
}
