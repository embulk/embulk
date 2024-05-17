package org.embulk.deps.config;

import java.io.InputStream;
import org.embulk.config.YamlProcessor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlProcessorImpl extends YamlProcessor {
    private final Yaml yaml;

    public YamlProcessorImpl(boolean withResolver) {
        if (withResolver) {
            final DumperOptions dumperOptions = new DumperOptions();
            final LoaderOptions loadingConfig = new LoaderOptions();
            this.yaml = new Yaml(
                    new SafeConstructor(loadingConfig),
                    new Representer(dumperOptions),
                    dumperOptions,
                    loadingConfig,
                    new YamlTagResolver());
        } else {
            this.yaml = new Yaml();
        }
    }

    @Override
    public String dump(Object data) {
        return yaml.dump(data);
    }

    @Override
    public Object load(InputStream data) {
        return yaml.load(data);
    }

    @Override
    public Object load(String data) {
        return yaml.load(data);
    }
}
