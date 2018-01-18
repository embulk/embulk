package org.embulk.spi.time;

import org.embulk.spi.DataException;

public class TimestampParseException extends DataException {
    public TimestampParseException(String message) {
        super(message);
    }

    public TimestampParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimestampParseException(Throwable cause) {
        super(cause);
    }
}
