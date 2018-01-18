package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Extension;

public class StandardPluginExtension implements Extension {
    public List<Module> getModules(ConfigSource systemConfig) {
        return ImmutableList.<Module>of(new StandardPluginModule());
    }
}
