package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.SchemaConfigException;
import org.embulk.spi.type.Type;
import org.embulk.standards.RenameFilterPlugin.PluginTask;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.embulk.spi.type.Types.STRING;
import static org.embulk.spi.type.Types.TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestRenameFilterPlugin
{
    @Rule
    public ExpectedException thrown= ExpectedException.none();
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private RenameFilterPlugin filter;
    private Schema inputSchema;
    private ConfigSource pluginConfig;

    @Before
    public void createResources()
    {
        filter = new RenameFilterPlugin();

        // input schema
        inputSchema = newSchema(
                newColumn("_c0", STRING),
                newColumn("_c1", TIMESTAMP));
    }

    @Test
    public void checkDefaultValues()
    {
        ConfigSource config = Exec.newConfigSource();
        PluginTask task = config.loadConfig(PluginTask.class);
        assertTrue(task.getRenameMap().isEmpty());
    }
    private static Schema newSchema(ColumnConfig... columns)
    {
        return (new SchemaConfig(ImmutableList.copyOf(columns))).toSchema();
    }

    private static ColumnConfig newColumn(String name, Type type)
    {
        return new ColumnConfig(name, type, (String)null);
    }

    private static Map<String, String> newRenameMap(String... kvs)
    {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        for (int i = 0; i < kvs.length; i += 2) {
            builder.put(kvs[i], kvs[i+1]);
        }
        return builder.build();
    }

    @Test
    public void throwSchemaConfigIfColumnNotFound()
    {
        // config for plugin task
        pluginConfig = Exec.newConfigSource()
                .set("columns", newRenameMap("not_found", "any_name"));

        try {
            filter.transaction(pluginConfig, inputSchema, new FilterPlugin.Control() {
                @Override
                public void run(TaskSource task, Schema schema)
                {
                    // do nothing
                }
            });
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof SchemaConfigException);
        }
    }

    @Test
    public void checkRenaming()
    {
        // config for plugin task
        pluginConfig = Exec.newConfigSource()
                .set("columns", newRenameMap("_c0", "_cc0"));

        filter.transaction(pluginConfig, inputSchema, new FilterPlugin.Control() {
            @Override
            public void run(TaskSource task, Schema schema)
            {
                // _c0 -> _cc0
                Column old0 = inputSchema.getColumn(0);
                Column new0 = schema.getColumn(0);
                assertEquals("_cc0", new0.getName());
                assertEquals(old0.getType(), new0.getType());

                // _c1 is not changed
                Column old1 = inputSchema.getColumn(1);
                Column new1 = schema.getColumn(1);
                assertEquals("_c1", new1.getName());
                assertEquals(old1.getType(), new1.getType());
            }
        });
    }
}
