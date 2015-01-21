package org.embulk.standards;

import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import org.embulk.spi.Extension;
import org.embulk.config.ConfigSource;

public class StandardPluginExtension
        implements Extension
{
    public List<Module> getModules(ConfigSource systemConfig)
    {
        return ImmutableList.<Module>of(new StandardPluginModule());
    }
}
