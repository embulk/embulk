package org.embulk.deps.yaml;

import java.io.InputStream;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlProcessorImpl extends YamlProcessor {
    private final Yaml yaml;

    public YamlProcessorImpl(boolean withResolver) {
        if (withResolver) {
            this.yaml = new Yaml(new SafeConstructor(), new Representer(), new DumperOptions(), new YamlTagResolver());
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
