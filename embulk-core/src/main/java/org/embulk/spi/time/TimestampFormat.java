package org.embulk.spi.time;

// org.embulk.spi.time.TimestampFormat is deprecated.
// It won't be removed very soon at least until Embulk v0.10.
@Deprecated
public class TimestampFormat {
    public TimestampFormat(final String format) {
        this.format = format;
    }

    public String getFormat() {
        return this.format;
    }

    private final String format;
}
