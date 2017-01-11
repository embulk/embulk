package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;
import org.embulk.standards.RemoveColumnsFilterPlugin.PluginTask;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.embulk.spi.Exec.newConfigSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestRemoveColumnsFilterPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private RemoveColumnsFilterPlugin plugin;
    private Schema inputSchema;

    @Before
    public void createResources()
    {
        plugin = new RemoveColumnsFilterPlugin();
        inputSchema = Schema.builder()
                .add("_c0", Types.STRING)
                .add("_c1", Types.STRING)
                .add("_c2", Types.STRING)
                .add("_c3", Types.STRING)
                .add("_c4", Types.STRING)
                .build();
    }

    @Test
    public void checkDefaultValues()
    {
        PluginTask task = newConfigSource().loadConfig(PluginTask.class);
        assertTrue(!task.getRemove().isPresent());
        assertTrue(!task.getKeep().isPresent());
        assertFalse(task.getAcceptUnmatchedColumns());
    }

    @Test
    public void checkValidation()
    {
        // throw ConfigError if remove: and keep: are multi-select.
        {
            ConfigSource config = newConfigSource()
                    .set("remove", ImmutableList.of("_c0", "_c1"))
                    .set("keep", ImmutableList.of("_c2", "_c3"));
            try {
                transaction(config, inputSchema);
                fail();
            }
            catch (Throwable t) {
                assertTrue(t instanceof ConfigException);
            }
        }

        // throw ConfigError if an user dones't use both of them.
        {
            ConfigSource config = newConfigSource();
            try {
                transaction(config, inputSchema);
                fail();
            }
            catch (Throwable t) {
                assertTrue(t instanceof ConfigException);
            }
        }

        // throw ConfigError if keep: has unmatched columns with accept_unmatched_columns=false
        {
            ConfigSource config = newConfigSource()
                    .set("keep", ImmutableList.of("_c5"))
                    .set("accept_unmatched_columns", false);
            try {
                transaction(config, inputSchema);
                fail();
            }
            catch (Throwable t) {
                assertTrue(t instanceof ConfigException);
            }
        }

        // return normally if keep: has unmatched columns with accept_unmatched_columns=true
        {
            ConfigSource config = newConfigSource()
                    .set("keep", ImmutableList.of("_c5"))
                    .set("accept_unmatched_columns", true);
            transaction(config, inputSchema); // return normally
        }

        // throw ConfigError if remove: has unmatched columns with accept_unmatched_columns=false
        {
            ConfigSource config = newConfigSource()
                    .set("remove", ImmutableList.of("_c5"))
                    .set("accept_unmatched_columns", false);
            try {
                transaction(config, inputSchema);
                fail();
            }
            catch (Throwable t) {
                assertTrue(t instanceof ConfigException);
            }
        }

        // return normally if remove: has unmatched columns with accept_unmatched_columns=true
        {
            ConfigSource config = newConfigSource()
                    .set("remove", ImmutableList.of("_c5"))
                    .set("accept_unmatched_columns", true);
            transaction(config, inputSchema); // return normally
        }
    }

    @Test
    public void checkRemove()
    {
        // accept_unmatched_columns=false
        {
            ConfigSource config = newConfigSource()
                    .set("remove", ImmutableList.of("_c2", "_c3", "_c4"))
                    .set("accept_unmatched_columns", false);
            plugin.transaction(config, inputSchema, new FilterPlugin.Control() {
                @Override
                public void run(TaskSource taskSource, Schema outputSchema)
                {
                    assertEquals(2, outputSchema.getColumnCount());
                    assertEquals("_c0", outputSchema.getColumn(0).getName());
                    assertEquals("_c1", outputSchema.getColumn(1).getName());
                }
            });
        }

        // accept_unmatched_columns=true
        {
            ConfigSource config = newConfigSource()
                    .set("remove", ImmutableList.of("_c2", "_c3", "_c4", "_c5", "_c6"))
                    .set("accept_unmatched_columns", true);
            plugin.transaction(config, inputSchema, new FilterPlugin.Control() {
                @Override
                public void run(TaskSource taskSource, Schema outputSchema)
                {
                    assertEquals(2, outputSchema.getColumnCount());
                    assertEquals("_c0", outputSchema.getColumn(0).getName());
                    assertEquals("_c1", outputSchema.getColumn(1).getName());
                }
            });
        }
    }

    @Test
    public void checkKeep()
    {
        // accept_unmatched_columns=false
        {
            ConfigSource config = newConfigSource()
                    .set("keep", ImmutableList.of("_c2", "_c3", "_c4"))
                    .set("accept_unmatched_columns", false);
            plugin.transaction(config, inputSchema, new FilterPlugin.Control() {
                @Override
                public void run(TaskSource taskSource, Schema outputSchema)
                {
                    assertEquals(3, outputSchema.getColumnCount());
                    assertEquals("_c2", outputSchema.getColumn(0).getName());
                    assertEquals("_c3", outputSchema.getColumn(1).getName());
                    assertEquals("_c4", outputSchema.getColumn(2).getName());
                }
            });
        }

        // accept_unmatched_columns=true
        {
            ConfigSource config = newConfigSource()
                    .set("keep", ImmutableList.of("_c2", "_c3", "_c4", "_c5", "_c6"))
                    .set("accept_unmatched_columns", true);
            plugin.transaction(config, inputSchema, new FilterPlugin.Control() {
                @Override
                public void run(TaskSource taskSource, Schema outputSchema)
                {
                    assertEquals(3, outputSchema.getColumnCount());
                    assertEquals("_c2", outputSchema.getColumn(0).getName());
                    assertEquals("_c3", outputSchema.getColumn(1).getName());
                    assertEquals("_c4", outputSchema.getColumn(2).getName());
                }
            });
        }
    }

    private void transaction(ConfigSource config, Schema inputSchema)
    {
        plugin.transaction(config, inputSchema, new FilterPlugin.Control() {
            @Override
            public void run(TaskSource taskSource, Schema outputSchema)
            {
                // do nothing
            }
        });
    }
}
