/*
 * Copyright 2017 The Embulk project
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

package org.embulk.standards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Exec;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfigException;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveColumnsFilterPlugin implements FilterPlugin {
    public interface PluginTask extends Task {
        @Config("remove")
        @ConfigDefault("null")
        public Optional<List<String>> getRemove();

        // TODO remove_pattern option

        @Config("keep")
        @ConfigDefault("null")
        public Optional<List<String>> getKeep();

        // TODO keep_pattern option

        @Config("accept_unmatched_columns")
        @ConfigDefault("false")
        public boolean getAcceptUnmatchedColumns();

        public void setIndexMapping(int[] mapping);

        public int[] getIndexMapping();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public void transaction(ConfigSource config, Schema inputSchema, FilterPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);

        // validate remove: and keep:
        if (task.getRemove().isPresent() && task.getKeep().isPresent()) {
            throw new ConfigException("remove: and keep: must not be multi-select");
        }
        if (!task.getRemove().isPresent() && !task.getKeep().isPresent()) {
            throw new ConfigException("Must require remove: or keep:");
        }

        boolean acceptUnmatchedColumns = task.getAcceptUnmatchedColumns();

        final ArrayList<Column> outputColumns = new ArrayList<>();
        int index = 0;
        int[] indexMapping = new int[inputSchema.size()];
        for (int i = 0; i < indexMapping.length; i++) {
            indexMapping[i] = -1;
        }
        if (task.getRemove().isPresent()) { // specify remove:
            List<String> removeColumns = getExistentColumns(inputSchema, task.getRemove().get(), acceptUnmatchedColumns);
            for (Column column : inputSchema.getColumns()) {
                if (!removeColumns.contains(column.getName())) {
                    outputColumns.add(new Column(index, column.getName(), column.getType()));
                    indexMapping[column.getIndex()] = index;
                    index++;
                }
            }
        } else { // specify keep:
            List<String> keepColumns = getExistentColumns(inputSchema, task.getKeep().get(), acceptUnmatchedColumns);
            for (Column column : inputSchema.getColumns()) {
                if (keepColumns.contains(column.getName())) {
                    outputColumns.add(new Column(index, column.getName(), column.getType()));
                    indexMapping[column.getIndex()] = index;
                    index++;
                }
            }
        }

        task.setIndexMapping(indexMapping);
        control.run(task.dump(), new Schema(Collections.unmodifiableList(outputColumns)));
    }

    private List<String> getExistentColumns(Schema schema, List<String> specifiedColumns, boolean acceptUnmatch) {
        final ArrayList<String> existentColumns = new ArrayList<>();
        for (String column : specifiedColumns) {
            try {
                schema.lookupColumn(column);
                existentColumns.add(column);
            } catch (SchemaConfigException e) {
                if (!acceptUnmatch) {
                    throw new ConfigException(String.format(Locale.ENGLISH, "Column '%s' doesn't exist in the schema", column));
                }
            }
        }
        return Collections.unmodifiableList(existentColumns);
    }

    @Override
    public PageOutput open(TaskSource taskSource, Schema inputSchema,
            Schema outputSchema, PageOutput output) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);
        final PageReader pageReader = getPageReader(inputSchema);
        final PageBuilder pageBuilder = getPageBuilder(Exec.getBufferAllocator(), outputSchema, output);
        return new PageConverter(pageReader, pageBuilder, task.getIndexMapping());
    }

    static class PageConverter implements PageOutput {
        private final PageReader pageReader;
        private final PageBuilder pageBuilder;
        private final int[] indexMapping;

        PageConverter(PageReader pageReader, PageBuilder pageBuilder, int[] indexMapping) {
            this.pageReader = pageReader;
            this.pageBuilder = pageBuilder;
            this.indexMapping = indexMapping;
        }

        @Override
        public void add(Page page) {
            pageReader.setPage(page);
            while (pageReader.nextRecord()) {
                pageReader.getSchema().visitColumns(new ColumnVisitor() {
                        @Override
                        public void booleanColumn(Column inputColumn) {
                            int index = indexMapping[inputColumn.getIndex()];
                            if (index >= 0) {
                                if (pageReader.isNull(inputColumn)) {
                                    pageBuilder.setNull(index);
                                } else {
                                    pageBuilder.setBoolean(index, pageReader.getBoolean(inputColumn));
                                }
                            }
                        }

                        @Override
                        public void longColumn(Column inputColumn) {
                            int index = indexMapping[inputColumn.getIndex()];
                            if (index >= 0) {
                                if (pageReader.isNull(inputColumn)) {
                                    pageBuilder.setNull(index);
                                } else {
                                    pageBuilder.setLong(index, pageReader.getLong(inputColumn));
                                }
                            }
                        }

                        @Override
                        public void doubleColumn(Column inputColumn) {
                            int index = indexMapping[inputColumn.getIndex()];
                            if (index >= 0) {
                                if (pageReader.isNull(inputColumn)) {
                                    pageBuilder.setNull(index);
                                } else {
                                    pageBuilder.setDouble(index, pageReader.getDouble(inputColumn));
                                }
                            }
                        }

                        @Override
                        public void stringColumn(Column inputColumn) {
                            int index = indexMapping[inputColumn.getIndex()];
                            if (index >= 0) {
                                if (pageReader.isNull(inputColumn)) {
                                    pageBuilder.setNull(index);
                                } else {
                                    pageBuilder.setString(index, pageReader.getString(inputColumn));
                                }
                            }
                        }

                        @Override
                        public void timestampColumn(Column inputColumn) {
                            int index = indexMapping[inputColumn.getIndex()];
                            if (index >= 0) {
                                if (pageReader.isNull(inputColumn)) {
                                    pageBuilder.setNull(index);
                                } else {
                                    setTimestamp(pageReader, inputColumn, pageBuilder, index);
                                }
                            }
                        }

                        @Override
                        public void jsonColumn(Column inputColumn) {
                            int index = indexMapping[inputColumn.getIndex()];
                            if (index >= 0) {
                                if (pageReader.isNull(inputColumn)) {
                                    pageBuilder.setNull(index);
                                } else {
                                    pageBuilder.setJson(index, pageReader.getJson(inputColumn));
                                }
                            }
                        }
                    });
                pageBuilder.addRecord();
            }
        }

        private Map<String, Integer> newColumnIndex(Schema schema) {
            final LinkedHashMap<String, Integer> builder = new LinkedHashMap<>();
            for (Column column : schema.getColumns()) {
                builder.put(column.getName(), column.getIndex());
            }
            return Collections.unmodifiableMap(builder);
        }

        @Override
        public void finish() {
            pageBuilder.finish();
        }

        @Override
        public void close() {
            pageBuilder.close();
        }
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder().
    private static PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        try {
            return Exec.getPageBuilder(allocator, schema, output);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageBuilder() is available from v0.10.17, and "new PageBuidler()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-filter-remove_columns is expected to work with Embulk v0.10.17+.", ex);
            return new PageBuilder(allocator, schema, output);
        }
    }

    @SuppressWarnings("deprecation")  // For the use of new PageReader().
    private static PageReader getPageReader(final Schema schema) {
        try {
            return Exec.getPageReader(schema);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageReader() is available from v0.10.17, and "new PageReader()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-filter-remove_columns is expected to work with Embulk v0.10.17+.", ex);
            return new PageReader(schema);
        }
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder and PageReader with java.time.Instant.
    private static void setTimestamp(
            final PageReader pageReader, final Column inputColumn, final PageBuilder pageBuilder, final int index) {
        try {
            pageBuilder.setTimestamp(index, pageReader.getTimestampInstant(inputColumn));
        } catch (final NoSuchMethodError ex) {
            // PageBuilder and PageReader with Instant are available from v0.10.13, and org.embulk.spi.Timestamp is deprecated.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-filter-remove_columns is expected to work with Embulk v0.10.17+.", ex);
            pageBuilder.setTimestamp(index, pageReader.getTimestamp(inputColumn));
        }
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    private static final Logger logger = LoggerFactory.getLogger(RemoveColumnsFilterPlugin.class);
}
