package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfigException;
import org.embulk.standards.RenameFilterPlugin.PluginTask;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;

import static org.embulk.spi.type.Types.STRING;
import static org.embulk.spi.type.Types.TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TestRenameFilterPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final Schema SCHEMA = Schema.builder()
            .add("_c0", STRING)
            .add("_c1", TIMESTAMP)
            .build();

    private final String DEFAULT = "__use_default__";

    private RenameFilterPlugin filter;

    @Before
    public void createFilter()
    {
        filter = new RenameFilterPlugin();
    }

    @Test
    public void checkDefaultValues()
    {
        PluginTask task = Exec.newConfigSource().loadConfig(PluginTask.class);
        assertTrue(task.getRenameMap().isEmpty());
    }

    @Test
    public void throwSchemaConfigExceptionIfColumnNotFound()
    {
        ConfigSource pluginConfig = Exec.newConfigSource()
                .set("columns", ImmutableMap.of("not_found", "any_name"));

        try {
            filter.transaction(pluginConfig, SCHEMA, new FilterPlugin.Control() {
                public void run(TaskSource task, Schema schema) { }
            });
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof SchemaConfigException);
        }
    }

    @Test
    public void checkRenaming()
    {
        ConfigSource pluginConfig = Exec.newConfigSource()
                .set("columns", ImmutableMap.of("_c0", "_c0_new"));

        filter.transaction(pluginConfig, SCHEMA, new FilterPlugin.Control() {
            @Override
            public void run(TaskSource task, Schema newSchema)
            {
                // _c0 -> _c0_new
                Column old0 = SCHEMA.getColumn(0);
                Column new0 = newSchema.getColumn(0);
                assertEquals("_c0_new", new0.getName());
                assertEquals(old0.getType(), new0.getType());

                // _c1 is not changed
                Column old1 = SCHEMA.getColumn(1);
                Column new1 = newSchema.getColumn(1);
                assertEquals("_c1", new1.getName());
                assertEquals(old1.getType(), new1.getType());
            }
        });
    }

    @Test
    public void checkConfigExceptionIfUnknownStringTypeOfRenamingOperator()
    {
        // A simple string shouldn't come as a renaming rule.
        ConfigSource pluginConfig = Exec.newConfigSource()
                .set("rules", ImmutableList.of("string_rule"));

        try {
            filter.transaction(pluginConfig, SCHEMA, new FilterPlugin.Control() {
                public void run(TaskSource task, Schema schema) { }
            });
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    @Test
    public void checkConfigExceptionIfUnknownListTypeOfRenamingOperator()
    {
        // A list [] shouldn't come as a renaming rule.
        ConfigSource pluginConfig = Exec.newConfigSource()
                .set("rules", ImmutableList.of(ImmutableList.of("listed_operator1", "listed_operator2")));

        try {
            filter.transaction(pluginConfig, SCHEMA, new FilterPlugin.Control() {
                public void run(TaskSource task, Schema schema) { }
            });
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    @Test
    public void checkConfigExceptionIfUnknownRenamingOperatorName()
    {
        ConfigSource pluginConfig = Exec.newConfigSource()
                .set("rules", ImmutableList.of(ImmutableMap.of("rule", "some_unknown_renaming_operator")));

        try {
            filter.transaction(pluginConfig, SCHEMA, new FilterPlugin.Control() {
                public void run(TaskSource task, Schema schema) { }
            });
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    @Test
    public void checkRuleLowerToUpperRule()
    {
        final String original[] = { "_C0", "_C1", "_c2" };
        final String expected[] = { "_C0", "_C1", "_C2" };
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.of("rule", "lower_to_upper")));
        renameAndCheckSchema(config, original, expected);
    }

    @Test
    public void checkTruncateRule()
    {
        final String original[] = { "foo", "bar", "foobar", "foobarbaz" };
        final String expected[] = { "foo", "bar", "foo",    "foo"       };
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.of("rule", "truncate", "max_length", "3")));
        renameAndCheckSchema(config, original, expected);
    }

    @Test
    public void checkTruncateRuleDefault()
    {
        final String original[] = {
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" };
        final String expected[] = {
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678" };
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.of("rule", "truncate")));
        renameAndCheckSchema(config, original, expected);
    }

    @Test
    public void checkRuleUpperToLowerRule()
    {
        final String original[] = { "_C0", "_C1", "_c2" };
        final String expected[] = { "_c0", "_c1", "_c2" };
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.of("rule", "upper_to_lower")));
        renameAndCheckSchema(config, original, expected);
    }

    public void checkCharacterTypesRulePassAlphabet()
    {
        final String original[] = { "Internal$Foo0123--Bar" };
        final String expected[] = { "Internal_Foo______Bar" };
        final String pass_types[] = { "a-z", "A-Z" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "");
    }

    @Test
    public void checkCharacterTypesRulePassAlphanumeric()
    {
        final String original[] = { "Internal$Foo0123--Bar" };
        final String expected[] = { "Internal_Foo0123__Bar" };
        final String pass_types[] = { "a-z", "A-Z", "0-9" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "");
    }

    @Test
    public void checkCharacterTypesRulePassLowercase()
    {
        final String original[] = { "Internal$Foo0123--Bar" };
        final String expected[] = { "_nternal__oo_______ar" };
        final String pass_types[] = { "a-z" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "");
    }

    @Test
    public void checkCharacterTypesRulePassLowerwording()
    {
        final String original[] = { "Internal$Foo_0123--Bar" };
        final String expected[] = { "-nternal--oo_0123---ar" };
        final String pass_types[] = { "a-z", "0-9" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "_", "-");
    }

    @Test
    public void checkCharacterTypesRulePassNumeric()
    {
        final String original[] = { "Internal$Foo_0123--Bar" };
        final String expected[] = { "_____________0123_____" };
        final String pass_types[] = { "0-9" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "");
    }

    @Test
    public void checkCharacterTypesRulePassUppercase()
    {
        final String original[] = { "Internal$Foo_0123--Bar" };
        final String expected[] = { "I________F_________B__" };
        final String pass_types[] = { "A-Z" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "");
    }

    @Test
    public void checkCharacterTypesRulePassUpperwording()
    {
        final String original[] = { "Internal$Foo_0123--Bar" };
        final String expected[] = { "I--------F--_0123--B--" };
        final String pass_types[] = { "A-Z", "0-9" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "_", "-");
    }

    @Test
    public void checkCharacterTypesRulePassWording()
    {
        final String original[] = { "Internal$Foo_0123--Bar" };
        final String expected[] = { "Internal-Foo_0123--Bar" };
        final String pass_types[] = { "a-z", "A-Z", "0-9" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "_", "-");
    }

    @Test
    public void checkCharacterTypesRulePassCombination()
    {
        final String original[] = { "@Foobar0123_$" };
        final String expected[] = { "__oobar0123__" };
        final String pass_types[] = { "0-9", "a-z" };
        checkCharacterTypesRuleInternal(original, expected, pass_types, "");
    }

    @Test
    public void checkCharacterTypesRuleLongReplace()
    {
        final String original[] = { "fooBAR" };
        final String pass_types[] = { "a-z" };
        exception.expect(IllegalArgumentException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkCharacterTypesRuleInternal(original, original, pass_types, "", "___");
    }

    @Test
    public void checkCharacterTypesRuleEmptyReplace()
    {
        final String original[] = { "fooBAR" };
        final String pass_types[] = { "a-z" };
        exception.expect(IllegalArgumentException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkCharacterTypesRuleInternal(original, original, pass_types, "", "");
    }

    // TODO(dmikurube): Test a nil/null replace.
    // - rule: character_types
    //   delimiter:

    @Test
    public void checkCharacterTypesRuleUnknownType()
    {
        final String original[] = { "fooBAR" };
        final String pass_types[] = { "some_unknown_keyword" };
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkCharacterTypesRuleInternal(original, original, pass_types, "");
    }

    private void checkCharacterTypesRuleInternal(
            final String original[],
            final String expected[],
            final String pass_types[],
            final String pass_characters)
    {
        checkCharacterTypesRuleInternal(original, expected, pass_types, pass_characters, DEFAULT);
    }

    private void checkCharacterTypesRuleInternal(
            final String original[],
            final String expected[],
            final String pass_types[],
            final String pass_characters,
            final String replace)
    {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "character_types");
        if (pass_types != null) {
            parameters.put("pass_types", ImmutableList.copyOf(pass_types));
        }
        if (!pass_characters.equals(DEFAULT)) {
            parameters.put("pass_characters", pass_characters);
        }
        if (!replace.equals(DEFAULT)) {
            parameters.put("replace", replace);
        }
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.copyOf(parameters)));
        renameAndCheckSchema(config, original, expected);
    }

    private Schema makeSchema(final String columnNames[])
    {
        Schema.Builder builder = new Schema.Builder();
        for (String columnName : columnNames) {
            builder.add(columnName, STRING);
        }
        return builder.build();
    }

    private void renameAndCheckSchema(ConfigSource config,
                                      final String original[],
                                      final String expected[])
    {
        final Schema originalSchema = makeSchema(original);
        filter.transaction(config, originalSchema, new FilterPlugin.Control() {
            @Override
            public void run(TaskSource task, Schema renamedSchema)
            {
                assertEquals(originalSchema.getColumnCount(), renamedSchema.getColumnCount());
                assertEquals(expected.length, renamedSchema.getColumnCount());
                for (int i = 0; i < renamedSchema.getColumnCount(); ++i) {
                    assertEquals(originalSchema.getColumnType(i), renamedSchema.getColumnType(i));
                    assertEquals(expected[i], renamedSchema.getColumnName(i));
                }
            }
        });
    }
}
