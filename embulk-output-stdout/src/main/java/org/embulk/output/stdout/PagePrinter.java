/*
 * Copyright 2015 The Embulk project
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

package org.embulk.output.stdout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.TimestampType;
import org.embulk.util.timestamp.TimestampFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A modified copy from embulk-core's org.embulk.spi.util.PagePrinter.
class PagePrinter {
    private final Schema schema;

    private final TimestampFormatter[] timestampFormatters;

    private final ArrayList<String> record;

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    public PagePrinter(final Schema schema, final String timeZoneId) {
        this.schema = schema;
        this.timestampFormatters = new TimestampFormatter[schema.getColumnCount()];
        for (int i = 0; i < timestampFormatters.length; i++) {
            if (schema.getColumnType(i) instanceof TimestampType) {
                TimestampType type = (TimestampType) schema.getColumnType(i);
                timestampFormatters[i] = TimestampFormatter.builder(getFormatFromTimestampTypeWithDeprecationSuppressed(type), true)
                        .setDefaultZoneFromString(timeZoneId)
                        .build();
            }
        }

        this.record = new ArrayList<String>(schema.getColumnCount());
        for (int i = 0; i < schema.getColumnCount(); i++) {
            record.add("");
        }
    }

    public String printRecord(PageReader reader, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Column c : schema.getColumns()) {
            if (c.getIndex() != 0) {
                sb.append(delimiter);
            }
            sb.append(printColumn(reader, c));
        }
        return sb.toString();
    }

    public List<String> printRecord(PageReader reader) {
        for (Column c : schema.getColumns()) {
            record.set(c.getIndex(), printColumn(reader, c));
        }
        return record;
    }

    public String printColumn(PageReader reader, Column column) {
        if (reader.isNull(column)) {
            return "";
        }
        ToStringColumnVisitor visitor = new ToStringColumnVisitor(reader);
        column.visit(visitor);
        return visitor.string;
    }

    private class ToStringColumnVisitor implements ColumnVisitor {
        private final PageReader reader;
        String string = "";

        public ToStringColumnVisitor(PageReader reader) {
            this.reader = reader;
        }

        public void booleanColumn(Column column) {
            string = Boolean.toString(reader.getBoolean(column));
        }

        public void longColumn(Column column) {
            string = Long.toString(reader.getLong(column));
        }

        public void doubleColumn(Column column) {
            string = Double.toString(reader.getDouble(column));
        }

        public void stringColumn(Column column) {
            string = reader.getString(column);
        }

        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
        public void timestampColumn(Column column) {
            string = timestampFormatters[column.getIndex()].format(getTimestamp(reader, column));
        }

        public void jsonColumn(Column column) {
            string = reader.getJson(column).toString();
        }
    }

    // TODO: Stop using TimestampType.getFormat.
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/935
    private String getFormatFromTimestampTypeWithDeprecationSuppressed(final TimestampType timestampType) {
        return timestampType.getFormat();
    }

    @SuppressWarnings("deprecation")  // For the use of new PageReader with java.time.Instant.
    private static Instant getTimestamp(final PageReader reader, final Column column) {
        try {
            return reader.getTimestampInstant(column);
        } catch (final NoSuchMethodError ex) {
            // PageReader with Instant are available from v0.10.13, and org.embulk.spi.Timestamp is deprecated.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-output-stdout is expected to work with Embulk v0.10.17+.", ex);
            return reader.getTimestamp(column).getInstant();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(PagePrinter.class);
}
