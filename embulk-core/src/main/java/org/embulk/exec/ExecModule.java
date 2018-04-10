package org.embulk.exec;

import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.embulk.config.ModelManager;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.time.DateTimeZoneSerDe;
import org.embulk.spi.time.TimestampSerDe;
import org.embulk.spi.unit.LocalFileSerDe;
import org.embulk.spi.util.CharsetSerDe;
import org.slf4j.ILoggerFactory;

public class ExecModule implements Module {
    @Override
    public void configure(Binder binder) {
        Preconditions.checkNotNull(binder, "binder is null.");

        binder.bind(BulkLoader.class);

        binder.bind(ILoggerFactory.class).toProvider(LoggerProvider.class).in(Scopes.SINGLETON);
        binder.bind(ModelManager.class).in(Scopes.SINGLETON);
        binder.bind(BufferAllocator.class).to(PooledBufferAllocator.class).in(Scopes.SINGLETON);
        binder.bind(TempFileAllocator.class).in(Scopes.SINGLETON);

        // GuessExecutor, PreviewExecutor
        registerPluginTo(binder, ParserPlugin.class, "system_guess", GuessExecutor.GuessParserPlugin.class);
        registerPluginTo(binder, ParserPlugin.class, "system_sampling", SamplingParserPlugin.class);

        // LocalExecutorPlugin
        registerPluginTo(binder, ExecutorPlugin.class, "local", LocalExecutorPlugin.class);

        // serde
        ObjectMapperModule mapper = new ObjectMapperModule();
        DateTimeZoneSerDe.configure(mapper);
        TimestampSerDe.configure(mapper);
        CharsetSerDe.configure(mapper);
        LocalFileSerDe.configure(mapper);
        mapper.registerModule(new GuavaModule());  // jackson-datatype-guava
        mapper.registerModule(new Jdk8Module());  // jackson-datatype-jdk8
        mapper.registerModule(new JodaModule());  // jackson-datatype-joda
        mapper.configure(binder);
    }
}
