package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskValidationException;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import static org.embulk.spi.type.Types.STRING;
import static org.embulk.spi.type.Types.TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing |RenameFilterPlugin|.
 *
 * NOTE: DO NOT CHANGE THE EXISTING TESTS.
 *
 * As written in the comment of |RenameFilterPlugin|, the existing behaviors
 * should not change so that users are not confused.
 */
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
        final String original[] = { "foo", "bar", "gj", "foobar", "foobarbaz" };
        final String expected[] = { "foo", "bar", "gj", "foo",    "foo"       };
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
    public void checkTruncateRuleNegative()
    {
        final String original[] = { "foo" };
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.of("rule", "truncate", "max_length", -1)));
        exception.expect(TaskValidationException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        renameAndCheckSchema(config, original, original);
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

    @Test
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
        exception.expect(TaskValidationException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkCharacterTypesRuleInternal(original, original, pass_types, "", "___");
    }

    @Test
    public void checkCharacterTypesRuleEmptyReplace()
    {
        final String original[] = { "fooBAR" };
        final String pass_types[] = { "a-z" };
        exception.expect(TaskValidationException.class);
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

    @Test
    public void checkCharacterTypesRuleForbiddenCharSequence()
    {
        final String original[] = { "fooBAR" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkCharacterTypesRuleInternal(original, original, pass_types, "\\E");
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

    @Test
    public void checkRegexReplaceRule1()
    {
        final String original[] = { "foobarbaz" };
        final String expected[] = { "hogebarbaz" };
        checkRegexReplaceRuleInternal(original, expected, "foo", "hoge");
    }

    @Test
    public void checkRegexReplaceRule2()
    {
        final String original[] = { "200_dollars" };
        final String expected[] = { "USD200" };
        checkRegexReplaceRuleInternal(original, expected, "([0-9]+)_dollars", "USD$1");
    }

    private void checkRegexReplaceRuleInternal(
            final String original[],
            final String expected[],
            final String match,
            final String replace)
    {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "regex_replace");
        parameters.put("match", match);
        parameters.put("replace", replace);
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.copyOf(parameters)));
        renameAndCheckSchema(config, original, expected);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceSingleHyphen()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "_oo", "_12foo", "_bar", "_AZ", "_ban", "_jar", "_zip", "-zap" };
        final String pass_types[] = {};
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types, "-");
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceMultipleSingles()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "_oo", "_12foo", "@bar", "_AZ", "_ban", "_jar", "*zip", "-zap" };
        final String pass_types[] = {};
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types, "-@*");
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceAlphabet()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "foo", "_12foo", "_bar", "BAZ", "_ban", "_jar", "_zip", "_zap" };
        final String pass_types[] = { "a-z", "A-Z" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceAlphanumeric()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "foo", "012foo", "_bar", "BAZ", "_ban", "_jar", "_zip", "_zap" };
        final String pass_types[] = { "a-z", "A-Z", "0-9" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceLowercase()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "foo", "_12foo", "_bar", "_AZ", "_ban", "_jar", "_zip", "_zap" };
        final String pass_types[] = { "a-z" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceLowerwording()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "foo", "012foo", "-bar", "-AZ", "-ban", "_jar", "-zip", "-zap" };
        final String pass_types[] = { "a-z", "0-9" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "-", pass_types, "_");
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceNumeric()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "_oo", "012foo", "_bar", "_AZ", "_ban", "_jar", "_zip", "_zap" };
        final String pass_types[] = { "0-9" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceUppercase()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "_oo", "_12foo", "_bar", "BAZ", "_ban", "_jar", "_zip", "_zap" };
        final String pass_types[] = { "A-Z" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceUpperwording()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "-oo", "012foo", "-bar", "BAZ", "-ban", "_jar", "-zip", "-zap" };
        final String pass_types[] = { "A-Z", "0-9" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "-", pass_types, "_");
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceWording()
    {
        final String original[] = { "foo", "012foo", "@bar", "BAZ", "&ban", "_jar", "*zip", "-zap" };
        final String expected[] = { "foo", "012foo", "$bar", "BAZ", "$ban", "_jar", "$zip", "$zap" };
        final String pass_types[] = { "a-z", "A-Z", "0-9" };
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, "$", pass_types, "_");
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceUnknownFirst()
    {
        final String original[] = { "foo" };
        final String pass_types[] = { "some_unknown_type" };
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRuleReplaceInternal(original, original, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleReplaceForbiddenCharSequence()
    {
        final String original[] = { "foo" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRuleReplaceInternal(original, original, "_", pass_types, "\\E");
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixSingleHyphen()
    {
        final String original[] = { "foo",  "012foo",  "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "_foo", "_012foo", "_@bar", "_BAZ", "_&ban", "__jar", "_*zip",  "-zap" };
        final String pass_types[] = {};
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types, "-");
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixMultipleSingles()
    {
        final String original[] = { "foo",  "012foo",  "@bar", "BAZ",  "&ban",  "_jar",  "*zip", "-zap"  };
        final String expected[] = { "_foo", "_012foo", "@bar", "_BAZ", "_&ban", "__jar", "*zip", "-zap" };
        final String pass_types[] = {};
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types, "-@*");
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixAlphabet()
    {
        final String original[] = { "foo", "012foo",  "@bar",  "BAZ", "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "foo", "_012foo", "_@bar", "BAZ", "_&ban", "__jar", "_*zip", "_-zap" };
        final String pass_types[] = { "a-z", "A-Z" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixAlphanumeric()
    {
        final String original[] = { "foo", "012foo", "@bar",  "BAZ", "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "foo", "012foo", "_@bar", "BAZ", "_&ban", "__jar", "_*zip", "_-zap" };
        final String pass_types[] = { "a-z", "A-Z", "0-9" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixLowercase()
    {
        final String original[] = { "foo", "012foo",  "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "foo", "_012foo", "_@bar", "_BAZ", "_&ban", "__jar", "_*zip", "_-zap" };
        final String pass_types[] = { "a-z" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixLowerwording()
    {
        final String original[] = { "foo", "012foo", "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "foo", "012foo", "-@bar", "-BAZ", "-&ban", "_jar", "-*zip", "--zap" };
        final String pass_types[] = { "a-z", "0-9" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "-", pass_types, "_");
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixNumeric()
    {
        final String original[] = { "foo",  "012foo", "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "_foo", "012foo", "_@bar", "_BAZ", "_&ban", "__jar", "_*zip", "_-zap" };
        final String pass_types[] = { "0-9" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixUppercase()
    {
        final String original[] = { "foo",  "012foo",  "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "_foo", "_012foo", "_@bar", "BAZ",  "_&ban", "__jar", "_*zip", "_-zap" };
        final String pass_types[] = { "A-Z" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixUpperwording()
    {
        final String original[] = { "foo",  "012foo",  "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "-foo", "012foo",  "-@bar", "BAZ",  "-&ban", "_jar",  "-*zip", "--zap" };
        final String pass_types[] = { "A-Z", "0-9" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "-", pass_types, "_");
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixWording()
    {
        final String original[] = { "foo",  "012foo",  "@bar",  "BAZ",  "&ban",  "_jar",  "*zip",  "-zap"  };
        final String expected[] = { "foo",  "012foo",  "$@bar", "BAZ",  "$&ban", "_jar",  "$*zip", "$-zap" };
        final String pass_types[] = { "a-z", "A-Z", "0-9" };
        checkFirstCharacterTypesRulePrefixInternal(original, expected, "$", pass_types, "_");
    }

    @Test
    public void checkFirstCharacterTypesRuleEmptyPrefix()
    {
        final String original[] = { "foo" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRulePrefixInternal(original, original, "", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleLongPrefix()
    {
        final String original[] = { "foo" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRulePrefixInternal(original, original, "__", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleEmptyReplace()
    {
        final String original[] = { "foo" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRuleReplaceInternal(original, original, "", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleLongReplace()
    {
        final String original[] = { "foo" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRuleReplaceInternal(original, original, "__", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixUnknownFirst()
    {
        final String original[] = { "foo" };
        final String pass_types[] = { "some_unknown_type" };
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRulePrefixInternal(original, original, "_", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRulePrefixForbiddenCharSequence()
    {
        final String original[] = { "foo" };
        final String pass_types[] = {};
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkFirstCharacterTypesRulePrefixInternal(original, original, "\\E", pass_types);
    }

    @Test
    public void checkFirstCharacterTypesRuleBothReplacePrefix()
    {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "first_character_types");
        parameters.put("replace", "_");
        parameters.put("prefix", "_");
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.copyOf(parameters)));
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        renameAndCheckSchema(config, new String[0], new String[0]);
    }

    @Test
    public void checkFirstCharacterTypesRuleNeitherReplacePrefix()
    {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "first_character_types");
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.copyOf(parameters)));
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        renameAndCheckSchema(config, new String[0], new String[0]);
    }

    private void checkFirstCharacterTypesRuleReplaceInternal(
            final String original[],
            final String expected[],
            final String replace,
            final String pass_types[]) {
        checkFirstCharacterTypesRuleReplaceInternal(original, expected, replace, pass_types, DEFAULT);
    }

    private void checkFirstCharacterTypesRuleReplaceInternal(
            final String original[],
            final String expected[],
            final String replace,
            final String pass_types[],
            final String pass_characters)
    {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "first_character_types");
        if (pass_types.length > 0) {
            parameters.put("pass_types", Arrays.asList(pass_types));
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

    private void checkFirstCharacterTypesRulePrefixInternal(
            final String original[],
            final String expected[],
            final String prefix,
            final String pass_types[]) {
        checkFirstCharacterTypesRulePrefixInternal(original, expected, prefix, pass_types, DEFAULT);
    }

    private void checkFirstCharacterTypesRulePrefixInternal(
            final String original[],
            final String expected[],
            final String prefix,
            final String pass_types[],
            final String pass_characters)
    {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "first_character_types");
        if (pass_types.length > 0) {
            parameters.put("pass_types", Arrays.asList(pass_types));
        }
        if (!pass_characters.equals(DEFAULT)) {
            parameters.put("pass_characters", pass_characters);
        }
        if (!prefix.equals(DEFAULT)) {
            parameters.put("prefix", prefix);
        }
        ConfigSource config = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.copyOf(parameters)));
        renameAndCheckSchema(config, original, expected);
    }

    public void checkUniqueNumberSuffixRuleEmptyDelimiter()
    {
        final String columnNames[] = { "c" };
        try {
            checkUniqueNumberSuffixRuleInternal(columnNames, columnNames, "");
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    @Test
    public void checkUniqueNumberSuffixRuleLongDelimiter()
    {
        final String columnNames[] = { "c" };
        try {
            checkUniqueNumberSuffixRuleInternal(columnNames, columnNames, "__");
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    @Test
    public void checkUniqueNumberSuffixRuleDigitDelimiter()
    {
        final String columnNames[] = { "c" };
        try {
            checkUniqueNumberSuffixRuleInternal(columnNames, columnNames, "2");
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    @Test
    public void checkUniqueNumberSuffixRuleShortMaxLength()
    {
        final String columnNames[] = { "c" };
        try {
            checkUniqueNumberSuffixRuleInternal(columnNames, columnNames, DEFAULT, -1, 7);
        } catch (Throwable t) {
            assertTrue(t instanceof ConfigException);
        }
    }

    // TODO(dmikurube): Test a nil/null delimiter in "unique".
    // - rule: unique
    //   delimiter:

    @Test
    public void checkUniqueNumberSuffixRule0()
    {
        final String originalColumnNames[] = { "a", "b", "c", "d", "e" };
        final String expectedColumnNames[] = { "a", "b", "c", "d", "e" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames);
    }

    @Test
    public void checkUniqueNumberSuffixRule1()
    {
        final String originalColumnNames[] = { "c", "c",   "c1", "c2", "c2"   };
        final String expectedColumnNames[] = { "c", "c_2", "c1", "c2", "c2_2" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames);
    }

    @Test
    public void checkUniqueNumberSuffixRule2()
    {
        final String originalColumnNames[] = { "c", "c",   "c_1", "c_3", "c"   };
        final String expectedColumnNames[] = { "c", "c_2", "c_1", "c_3", "c_4" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames);
    }

    @Test
    public void checkUniqueNumberSuffixRule3()
    {
        final String originalColumnNames[] = {
            "c", "c",   "c",   "c",   "c",   "c",   "c",   "c",   "c",   "c",    "c_1", "c_1"   };
        final String expectedColumnNames[] = {
            "c", "c_2", "c_3", "c_4", "c_5", "c_6", "c_7", "c_8", "c_9", "c_10", "c_1", "c_1_2" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames);
    }

    @Test
    public void checkUniqueNumberSuffixRule4DifferentDelimiter()
    {
        final String originalColumnNames[] = { "c", "c",   "c1", "c2", "c2"   };
        final String expectedColumnNames[] = { "c", "c-2", "c1", "c2", "c2-2" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, "-");
    }

    @Test
    public void checkUniqueNumberSuffixRule5Digits()
    {
        final String originalColumnNames[] = { "c", "c",      "c1", "c2", "c2"   };
        final String expectedColumnNames[] = { "c", "c_0002", "c1", "c2", "c2_0002" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, DEFAULT, 4, -1);
    }

    @Test
    public void checkUniqueNumberSuffixRule6MaxLength1()
    {
        final String originalColumnNames[] = { "column", "column",   "column_1", "column_2", "column_2" };
        final String expectedColumnNames[] = { "column", "column_3", "column_1", "column_2", "column_4" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, DEFAULT, -1, 8);
    }

    @Test
    public void checkUniqueNumberSuffixRule7()
    {
        final String originalColumnNames[] = { "column", "column",   "column_2", "column_3" };
        final String expectedColumnNames[] = { "column", "column_4", "column_2", "column_3" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, DEFAULT, -1, 8);
    }

    @Test
    public void checkUniqueNumberSuffixRule8MaxLength2()
    {
        final String originalColumnNames[] = {
            "column",   "colum",    "column",   "colum",    "column",   "colum",    "column",   "colum",    "column",
            "colum",    "column",   "colum",    "column",   "colum",    "column",   "colum",    "column",   "colum",
            "column",   "colum",    "column",   "colum"    };
        final String expectedColumnNames[] = {
            "column",   "colum",    "column_2", "colum_2",  "column_3", "colum_3",  "column_4", "colum_4",  "column_5",
            "colum_5",  "column_6", "colum_6",  "column_7", "colum_7",  "column_8", "colum_8",  "column_9", "colum_9",
            "colum_10", "colum_11", "colum_12", "colum_13" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, DEFAULT, -1, 8);
    }

    @Test
    public void checkUniqueNumberSuffixRule9MaxLength3()
    {
        final String originalColumnNames[] = {
            "column", "column",   "column",   "column",   "column",   "column",   "column",   "column",   "column",
            "colum",  "colum",    "colum",    "colum",    "colum",    "colum",    "colum",    "colum",
            "column",   "colum",   "column",   "colum",    "column"   };
        final String expectedColumnNames[] = {
            "column", "column_2", "column_3", "column_4", "column_5", "column_6", "column_7", "column_8", "column_9",
            "colum",  "colum_2",  "colum_3",  "colum_4",  "colum_5",  "colum_6",  "colum_7",  "colum_8",
            "colum_10", "colum_9", "colum_11", "colum_12", "colum_13" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, DEFAULT, -1, 8);
    }

    @Test
    public void checkUniqueNumberSuffixRule10EsteemOriginalNames()
    {
        final String originalColumnNames[] = { "c", "c",   "c_2" };
        final String expectedColumnNames[] = { "c", "c_3", "c_2" };
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, expectedColumnNames, DEFAULT, -1, -1);
    }

    @Test
    public void checkUniqueNumberSuffixRuleNegativeLength()
    {
        final String originalColumnNames[] = { "column" };
        exception.expect(ConfigException.class);
        // TODO(dmikurube): Except "Caused by": exception.expectCause(instanceOf(JsonMappingException.class));
        // Needs to import org.hamcrest.Matchers... in addition to org.junit...
        checkUniqueNumberSuffixRuleInternal(originalColumnNames, originalColumnNames, DEFAULT, -1, -2);
    }

    private void checkUniqueNumberSuffixRuleInternal(
            final String originalColumnNames[],
            final String expectedColumnNames[]) {
        checkUniqueNumberSuffixRuleInternal(originalColumnNames,
                                            expectedColumnNames,
                                            DEFAULT,
                                            -1,
                                            -1);
    }

    private void checkUniqueNumberSuffixRuleInternal(
            final String originalColumnNames[],
            final String expectedColumnNames[],
            String delimiter) {
        checkUniqueNumberSuffixRuleInternal(originalColumnNames,
                                            expectedColumnNames,
                                            delimiter,
                                            -1,
                                            -1);
    }

    private void checkUniqueNumberSuffixRuleInternal(
            final String originalColumnNames[],
            final String expectedColumnNames[],
            String delimiter,
            int digits,
            int max_length)
    {
        Schema.Builder originalSchemaBuilder = Schema.builder();
        for (String originalColumnName : originalColumnNames) {
            originalSchemaBuilder.add(originalColumnName, STRING);
        }
        final Schema ORIGINAL_SCHEMA = originalSchemaBuilder.build();

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("rule", "unique_number_suffix");
        if (!delimiter.equals(DEFAULT)) {
            parameters.put("delimiter", delimiter);
        }
        if (digits >= 0) {
            parameters.put("digits", digits);
        }
        if (max_length != -1) {
            parameters.put("max_length", max_length);
        }
        ConfigSource pluginConfig = Exec.newConfigSource().set("rules",
                ImmutableList.of(ImmutableMap.copyOf(parameters)));

        filter.transaction(pluginConfig, ORIGINAL_SCHEMA, new FilterPlugin.Control() {
            @Override
            public void run(TaskSource task, Schema newSchema)
            {
                ArrayList<String> resolvedColumnNamesList = new ArrayList<>(newSchema.size());
                for (Column resolvedColumn : newSchema.getColumns()) {
                    resolvedColumnNamesList.add(resolvedColumn.getName());
                }
                String[] resolvedColumnNames = Iterables.toArray(resolvedColumnNamesList, String.class);
                assertEquals(expectedColumnNames, resolvedColumnNames);
                for (int i = 0; i < expectedColumnNames.length; ++i) {
                    Column original = ORIGINAL_SCHEMA.getColumn(i);
                    Column resolved = newSchema.getColumn(i);
                    assertEquals(original.getType(), resolved.getType());
                }
            }
        });
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
