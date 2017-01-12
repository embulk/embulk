package org.embulk.standards;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
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
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static java.util.Locale.ENGLISH;
import static org.embulk.spi.Exec.getBufferAllocator;

public class RemoveColumnsFilterPlugin
        implements FilterPlugin
{
    public interface PluginTask
            extends Task
    {
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

    private final Logger LOG;

    @Inject
    public RemoveColumnsFilterPlugin()
    {
        LOG = Exec.getLogger(getClass());
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema, FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        // validate remove: and keep:
        if (task.getRemove().isPresent() && task.getKeep().isPresent()) {
            throw new ConfigException("remove: and keep: must not be multi-select");
        }
        if (!task.getRemove().isPresent() && !task.getKeep().isPresent()) {
            throw new ConfigException("Must require remove: or keep:");
        }

        boolean acceptUnmatchedColumns = task.getAcceptUnmatchedColumns();

        ImmutableList.Builder<Column> outputColumns = ImmutableList.builder();
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
        }
        else { // specify keep:
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
        control.run(task.dump(), new Schema(outputColumns.build()));
    }

    private List<String> getExistentColumns(Schema schema, List<String> specifiedColumns, boolean acceptUnmatch)
    {
        ImmutableList.Builder<String> existentColumns = ImmutableList.builder();
        for (String column : specifiedColumns) {
            try {
                schema.lookupColumn(column);
                existentColumns.add(column);
            }
            catch (SchemaConfigException e) {
                if (!acceptUnmatch) {
                    throw new ConfigException(String.format(ENGLISH, "Column '%s' doesn't exist in the schema", column));
                }
            }
        }
        return existentColumns.build();
    }

    @Override
    public PageOutput open(TaskSource taskSource, Schema inputSchema,
            Schema outputSchema, PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        PageReader pageReader = new PageReader(inputSchema);
        PageBuilder pageBuilder = new PageBuilder(getBufferAllocator(), outputSchema, output);
        return new PageConverter(pageReader, pageBuilder, task.getIndexMapping());
    }

    static class PageConverter
            implements PageOutput
    {
        private final PageReader pageReader;
        private final PageBuilder pageBuilder;
        private final int[] indexMapping;

        PageConverter(PageReader pageReader, PageBuilder pageBuilder, int[] indexMapping)
        {
            this.pageReader = pageReader;
            this.pageBuilder = pageBuilder;
            this.indexMapping = indexMapping;
        }

        @Override
        public void add(Page page)
        {
            pageReader.setPage(page);
            while (pageReader.nextRecord()) {
                pageReader.getSchema().visitColumns(new ColumnVisitor() {
                    @Override
                    public void booleanColumn(Column inputColumn)
                    {
                        int index = indexMapping[inputColumn.getIndex()];
                        if (index >= 0) {
                            if (pageReader.isNull(inputColumn)) {
                                pageBuilder.setNull(index);
                            }
                            else {
                                pageBuilder.setBoolean(index, pageReader.getBoolean(inputColumn));
                            }
                        }
                    }

                    @Override
                    public void longColumn(Column inputColumn)
                    {
                        int index = indexMapping[inputColumn.getIndex()];
                        if (index >= 0) {
                            if (pageReader.isNull(inputColumn)) {
                                pageBuilder.setNull(index);
                            }
                            else {
                                pageBuilder.setLong(index, pageReader.getLong(inputColumn));
                            }
                        }
                    }

                    @Override
                    public void doubleColumn(Column inputColumn)
                    {
                        int index = indexMapping[inputColumn.getIndex()];
                        if (index >= 0) {
                            if (pageReader.isNull(inputColumn)) {
                                pageBuilder.setNull(index);
                            }
                            else {
                                pageBuilder.setDouble(index, pageReader.getDouble(inputColumn));
                            }
                        }
                    }

                    @Override
                    public void stringColumn(Column inputColumn)
                    {
                        int index = indexMapping[inputColumn.getIndex()];
                        if (index >= 0) {
                            if (pageReader.isNull(inputColumn)) {
                                pageBuilder.setNull(index);
                            }
                            else {
                                pageBuilder.setString(index, pageReader.getString(inputColumn));
                            }
                        }
                    }

                    @Override
                    public void timestampColumn(Column inputColumn)
                    {
                        int index = indexMapping[inputColumn.getIndex()];
                        if (index >= 0) {
                            if (pageReader.isNull(inputColumn)) {
                                pageBuilder.setNull(index);
                            }
                            else {
                                pageBuilder.setTimestamp(index, pageReader.getTimestamp(inputColumn));
                            }
                        }
                    }

                    @Override
                    public void jsonColumn(Column inputColumn)
                    {
                        int index = indexMapping[inputColumn.getIndex()];
                        if (index >= 0) {
                            if (pageReader.isNull(inputColumn)) {
                                pageBuilder.setNull(index);
                            }
                            else {
                                pageBuilder.setJson(index, pageReader.getJson(inputColumn));
                            }
                        }
                    }
                });
                pageBuilder.addRecord();
            }
        }

        private Map<String, Integer> newColumnIndex(Schema schema)
        {
            ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
            for (Column column : schema.getColumns()) {
                builder.put(column.getName(), column.getIndex());
            }
            return builder.build();
        }

        @Override
        public void finish()
        {
            pageBuilder.finish();
        }

        @Override
        public void close()
        {
            pageBuilder.close();
        }
    }
}
