package org.embulk.exec;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.embulk.EmbulkSystemProperties;

public class ExecModule implements Module {
    public ExecModule(final EmbulkSystemProperties embulkSystemProperties) {
        this.embulkSystemProperties = embulkSystemProperties;
    }

    // DateTimeZoneJacksonModule, TimestampJacksonModule, ToStringJacksonModule, ToStringMapJacksonModule
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304 and Jackson Modules
    @Override
    public void configure(final Binder binder) {
        if (binder == null) {
            throw new NullPointerException("binder is null.");
        }
    }

    private final EmbulkSystemProperties embulkSystemProperties;
}
