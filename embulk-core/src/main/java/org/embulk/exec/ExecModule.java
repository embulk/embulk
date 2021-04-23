package org.embulk.exec;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.embulk.EmbulkSystemProperties;
import org.slf4j.ILoggerFactory;

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

        // TODO: Remove this ILoggerFactory binding.
        binder.bind(ILoggerFactory.class).toProvider(LoggerProvider.class).in(Scopes.SINGLETON);
    }

    private final EmbulkSystemProperties embulkSystemProperties;
}
