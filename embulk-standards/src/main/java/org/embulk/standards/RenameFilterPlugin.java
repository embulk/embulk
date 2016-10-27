package org.embulk.standards;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Locale;
import java.util.Map;


public class RenameFilterPlugin
        implements FilterPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("columns")
        @ConfigDefault("{}")
        Map<String, String> getRenameMap();

        @Config("rules")
        @ConfigDefault("[]")
        List<ConfigSource> getRulesList();
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema,
                            FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        Map<String, String> renameMap = task.getRenameMap();
        List<ConfigSource> rulesList = task.getRulesList();

        // Check if the given column in "columns" exists or not.
        for (String columnName : renameMap.keySet()) {
            inputSchema.lookupColumn(columnName); // throws SchemaConfigException
        }

        // Rename by "columns": to be applied before "rules".
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            String name = column.getName();
            if (renameMap.containsKey(name)) {
                name = renameMap.get(name);
            }
            builder.add(name, column.getType());
        }
        Schema intermediateSchema = builder.build();

        // Rename by "rules".
        Schema outputSchema = intermediateSchema;
        for (ConfigSource rule : rulesList) {
            outputSchema = applyRule(rule, intermediateSchema);
            intermediateSchema = outputSchema;
        }

        control.run(task.dump(), outputSchema);
    }

    @Override
    public PageOutput open(TaskSource taskSource, Schema inputSchema,
                           Schema outputSchema, PageOutput output)
    {
        return output;
    }

    // Extending Task is required to be deserialized with ConfigSource.loadConfig()
    // although this Rule is not really a Task.
    // TODO(dmikurube): Revisit this to consider how not to extend Task for this.
    private interface Rule
            extends Task
    {
        @Config("rule")
        String getRule();
    }

    private interface CharacterTypesRule
            extends Rule {
        @Config("pass_types")
        @ConfigDefault("[]")
        List<String> getPassTypes();

        @Config("pass_characters")
        @ConfigDefault("\"\"")
        String getPassCharacters();

        @Config("replace")
        @ConfigDefault("\"_\"")
        String getReplace();
    }

    private interface TruncateRule
            extends Rule {
        @Config("max_length")
        @ConfigDefault("128")
        int getMaxLength();
    }

    private Schema applyRule(ConfigSource ruleConfig, Schema inputSchema) throws ConfigException
    {
        Rule rule = ruleConfig.loadConfig(Rule.class);
        switch (rule.getRule()) {
        case "character_types":
            return applyCharacterTypesRule(inputSchema, ruleConfig.loadConfig(CharacterTypesRule.class));
        case "lower_to_upper":
            return applyLowerToUpperRule(inputSchema);
        case "truncate":
            return applyTruncateRule(inputSchema, ruleConfig.loadConfig(TruncateRule.class));
        case "upper_to_lower":
            return applyUpperToLowerRule(inputSchema);
        default:
            throw new ConfigException("Renaming rule \"" +rule+ "\" is unknown");
        }
    }

    private Schema applyCharacterTypesRule(Schema inputSchema, CharacterTypesRule rule) {
        final List<String> passTypes = rule.getPassTypes();
        final String passCharacters = rule.getPassCharacters();
        final String replace = rule.getReplace();

        Preconditions.checkNotNull(replace, "\"replace\" in \"character_types\" must not be explicitly empty");
        Preconditions.checkArgument(replace.length() == 1,
                                    "\"replace\" in \"character_types\" must contain just 1 character");
        // TODO(dmikurube): Revisit this for better escaping.
        Preconditions.checkArgument(!passCharacters.contains("\\E"),
                                    "\"pass_characters\" in \"character_types\" must not contain \"\\E\"");

        StringBuilder regexBuilder = new StringBuilder();
        regexBuilder.append("[^");
        for (String target : passTypes) {
            if (CHARACTER_TYPE_KEYWORDS.containsKey(target)) {
                regexBuilder.append(CHARACTER_TYPE_KEYWORDS.get(target));
            } else {
                throw new ConfigException("\"" +target+ "\" is an unknown character type keyword");
            }
        }
        if (!passCharacters.isEmpty()) {
            regexBuilder.append("\\Q");
            regexBuilder.append(passCharacters);
            regexBuilder.append("\\E");
        }
        regexBuilder.append("]");

        Schema.Builder schemaBuilder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            schemaBuilder.add(column.getName().replaceAll(regexBuilder.toString(), replace), column.getType());
        }
        return schemaBuilder.build();
    }

    private Schema applyLowerToUpperRule(Schema inputSchema) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().toUpperCase(Locale.ENGLISH), column.getType());
        }
        return builder.build();
    }

    private Schema applyTruncateRule(Schema inputSchema, TruncateRule rule) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().substring(0, rule.getMaxLength()), column.getType());
        }
        return builder.build();
    }

    private Schema applyUpperToLowerRule(Schema inputSchema) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().toLowerCase(Locale.ENGLISH), column.getType());
        }
        return builder.build();
    }

    private final ImmutableMap<String, String> CHARACTER_TYPE_KEYWORDS = new ImmutableMap.Builder<String, String>()
        .put("a-z", "a-z")
        .put("A-Z", "A-Z")
        .put("0-9", "0-9")
        .build();
}
