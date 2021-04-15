package org.embulk.exec;

import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.CharsetJacksonModule;
import org.embulk.config.ColumnJacksonModule;
import org.embulk.config.LocalFileJacksonModule;
import org.embulk.config.SchemaJacksonModule;
import org.embulk.config.TimestampJacksonModule;
import org.embulk.config.ToStringJacksonModule;
import org.embulk.config.ToStringMapJacksonModule;
import org.embulk.config.TypeJacksonModule;
import org.embulk.deps.buffer.PooledBufferAllocator;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.TempFileSpaceAllocator;
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

        binder.bind(BulkLoader.class);

        // TODO: Remove this ILoggerFactory binding.
        binder.bind(ILoggerFactory.class).toProvider(LoggerProvider.class).in(Scopes.SINGLETON);

        binder.bind(BufferAllocator.class).toInstance(this.createBufferAllocatorFromSystemConfig());
        binder.bind(TempFileSpaceAllocator.class).toInstance(new SimpleTempFileSpaceAllocator());

        // TODO: Remove the ObjectMapperModule Guice module.
        // They should be no longer required once ModelManager is managed in ExecSessionInternal.
        final ObjectMapperModule mapper = new ObjectMapperModule();
        mapper.registerModule(new TimestampJacksonModule());  // Deprecated. TBD to remove or not.
        mapper.registerModule(new CharsetJacksonModule());
        mapper.registerModule(new LocalFileJacksonModule());
        mapper.registerModule(new ToStringJacksonModule());
        mapper.registerModule(new ToStringMapJacksonModule());
        mapper.registerModule(new TypeJacksonModule());
        mapper.registerModule(new ColumnJacksonModule());
        mapper.registerModule(new SchemaJacksonModule());
        mapper.registerModule(new GuavaModule());  // jackson-datatype-guava
        mapper.registerModule(new Jdk8Module());  // jackson-datatype-jdk8
        mapper.configure(binder);
    }

    private BufferAllocator createBufferAllocatorFromSystemConfig() {
        final String byteSizeRepresentation = this.embulkSystemProperties.getProperty("page_size");
        if (byteSizeRepresentation == null) {
            return PooledBufferAllocator.create();
        } else {
            final int byteSize = parseByteSizeRepresentation(byteSizeRepresentation);
            return PooledBufferAllocator.create(byteSize);
        }
    }

    private static int parseByteSizeRepresentation(final String byteSizeRepresentation) {
        if (byteSizeRepresentation == null) {  // Should not happen.
            throw new NullPointerException("size is null");
        }
        if (byteSizeRepresentation.isEmpty()) {
            throw new IllegalArgumentException("size is empty");
        }

        final Matcher matcher = BYTE_SIZE_PATTERN.matcher(byteSizeRepresentation);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size string '" + byteSizeRepresentation + "'");
        }

        final String numberPart = matcher.group(1);
        final String unitPart = matcher.group(2);

        final BigDecimal number = new BigDecimal(numberPart);  // NumberFormatException extends IllegalArgumentException.

        if (unitPart.isEmpty()) {
            return number.intValue();
        }

        switch (unitPart.toUpperCase(Locale.ENGLISH)) {
            case "B":
                return number.intValue();
            case "KB":
                return number.multiply(KILO).intValue();
            case "MB":
                return number.multiply(MEGA).intValue();
            case "GB":
                return number.multiply(GIGA).intValue();
            case "TB":
                return number.multiply(TERA).intValue();
            case "PB":
                return number.multiply(PETA).intValue();
            default:
                throw new IllegalArgumentException("Unknown unit '" + unitPart + "'");
        }
    }

    private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("\\A(\\d+(?:\\.\\d+)?)\\s?([a-zA-Z]*)\\z");

    private static final BigDecimal KILO = new BigDecimal(1L << 10);  // 1_024
    private static final BigDecimal MEGA = new BigDecimal(1L << 20);  // 1_048_576
    private static final BigDecimal GIGA = new BigDecimal(1L << 30);  // 1_073_741_824
    private static final BigDecimal TERA = new BigDecimal(1L << 40);  // 1_099_511_627_776
    private static final BigDecimal PETA = new BigDecimal(1L << 50);  // 1_125_899_906_842_624

    private final EmbulkSystemProperties embulkSystemProperties;
}
