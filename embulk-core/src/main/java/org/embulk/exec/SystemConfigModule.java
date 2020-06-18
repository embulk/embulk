package org.embulk.exec;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.EmbulkSystemProperties;

public class SystemConfigModule implements Module {
    private final EmbulkSystemProperties embulkSystemProperties;

    public SystemConfigModule(final EmbulkSystemProperties embulkSystemProperties) {
        this.embulkSystemProperties = embulkSystemProperties;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(EmbulkSystemProperties.class)
                .toInstance(embulkSystemProperties);
    }
}
