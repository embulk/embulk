package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Schema;
import org.embulk.spi.ColumnVisitor;

public class MockFormatterPlugin implements FormatterPlugin
{
    public static List<List<Object>> records;

    public interface PluginTask extends Task
    {
    }

    @Override
    public void transaction(ConfigSource config, Schema schema,
            FormatterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public PageOutput open(TaskSource taskSource, final Schema schema,
            FileOutput output)
    {
        return new PageOutput()
        {
            public void add(Page page)
            {
                records = readPage(schema, page);
            }

            @Override
            public void finish()
            {
            }

            @Override
            public void close()
            {
            }
        };
    }

    public static List<List<Object>> readPage(final Schema schema, Page page)
    {
        List<List<Object>> records = new ArrayList<>();
        try (final PageReader pageReader = new PageReader(schema)) {
            pageReader.setPage(page);
            while (pageReader.nextRecord()) {
                final List<Object> record = new ArrayList<>();
                schema.visitColumns(new ColumnVisitor()
                {
                    public void booleanColumn(Column column)
                    {
                        if (!pageReader.isNull(column)) {
                            record.add(pageReader.getBoolean(column));
                        }
                    }

                    public void longColumn(Column column)
                    {
                        if (!pageReader.isNull(column)) {
                            record.add(pageReader.getLong(column));
                        }
                    }

                    public void doubleColumn(Column column)
                    {
                        if (!pageReader.isNull(column)) {
                            record.add(pageReader.getDouble(column));
                        }
                    }

                    public void stringColumn(Column column)
                    {
                        if (!pageReader.isNull(column)) {
                            record.add(pageReader.getString(column));
                        }
                    }

                    public void timestampColumn(Column column)
                    {
                        if (!pageReader.isNull(column)) {
                            record.add(pageReader.getTimestamp(column));
                        }
                    }

                    public void jsonColumn(Column column)
                    {
                        if (!pageReader.isNull(column)) {
                            record.add(pageReader.getJson(column));
                        }
                    }
                });
                records.add(record);
            }
        }
        return records;
    }
}
