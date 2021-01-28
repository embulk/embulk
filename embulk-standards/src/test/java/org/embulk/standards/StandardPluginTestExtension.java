package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import java.util.List;
import org.embulk.EmbulkSystemProperties;
import org.embulk.spi.Extension;

public class StandardPluginTestExtension implements Extension {
    public List<Module> getModules(final EmbulkSystemProperties embulkSystemProperties) {
        return ImmutableList.<Module>of(new StandardPluginTestModule());
    }
}
