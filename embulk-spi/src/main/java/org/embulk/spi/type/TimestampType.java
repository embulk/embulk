/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi.type;

/**
 * Type class for Embulk's TIMESTAMP.
 *
 * <p>Plugins should not refer this class directly. Recommended to use constants in {@link Types} instead.
 *
 * @since 0.4.0
 */
@SuppressWarnings("deprecation")
public class TimestampType extends AbstractType {
    static final TimestampType TIMESTAMP = new TimestampType();

    private static final String DEFAULT_FORMAT = "%Y-%m-%d %H:%M:%S.%6N %z";

    private final String format;

    private TimestampType() {
        this(null);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    private TimestampType(String format) {
        super("timestamp", org.embulk.spi.time.Timestamp.class, 12);  // long msec + int nsec
        this.format = format;
    }

    /**
     * @since 0.4.0
     */
    @Deprecated
    public TimestampType withFormat(String format) {
        // TODO is this correct design...?
        return new TimestampType(format);
    }

    /**
     * @since 0.4.0
     */
    @Deprecated
    public String getFormat() {
        if (format == null) {
            return DEFAULT_FORMAT;
        } else {
            return format;
        }
    }
}
