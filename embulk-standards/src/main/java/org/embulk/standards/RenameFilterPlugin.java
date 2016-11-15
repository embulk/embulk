package org.embulk.standards;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * |RenameFilterPlugin| renames column names.
 *
 * NOTE: This filter should bahave always in the same way for the same configuration.
 * Changes in its behavior confuse users who are working with the same configuration.
 *
 * Even when a buggy behavior is found, fix it by:
 * 1) Adding a new option, and
 * 2) Implementing a new behavior in the new option.
 *
 * Keep the buggy behavior with the old configuration except for fatal failures so
 * that users are not confused.
 */
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
        @Size(min = 1, max = 1)
        String getReplace();
    }

    private interface FirstCharacterTypesRule
            extends Rule {
        @Config("replace")
        @ConfigDefault("null")
        Optional<String> getReplace();

        @Config("pass_types")
        @ConfigDefault("[]")
        List<String> getPassTypes();

        @Config("pass_characters")
        @ConfigDefault("\"\"")
        String getPassCharacters();

        @Config("prefix")
        @ConfigDefault("null")
        Optional<String> getPrefix();
    }

    private interface TruncateRule
            extends Rule {
        @Config("max_length")
        @ConfigDefault("128")
        @Min(0)
        int getMaxLength();
    }

    private interface RegexReplaceRule
            extends Rule {
        @Config("match")
        String getMatch();

        @Config("replace")
        String getReplace();
    }

    private interface UniqueNumberSuffixRule
            extends Rule {
        @Config("delimiter")
        @ConfigDefault("\"_\"")
        String getDelimiter();

        @Config("digits")
        @ConfigDefault("null")
        Optional<Integer> getDigits();

        @Config("max_length")
        @ConfigDefault("null")
        Optional<Integer> getMaxLength();

        @Config("offset")
        @ConfigDefault("1")
        @Min(0)
        int getOffset();
    }

    private Schema applyRule(ConfigSource ruleConfig, Schema inputSchema) throws ConfigException
    {
        Rule rule = ruleConfig.loadConfig(Rule.class);
        switch (rule.getRule()) {
        case "character_types":
            return applyCharacterTypesRule(inputSchema, ruleConfig.loadConfig(CharacterTypesRule.class));
        case "first_character_types":
            return applyFirstCharacterTypesRule(inputSchema, ruleConfig.loadConfig(FirstCharacterTypesRule.class));
        case "lower_to_upper":
            return applyLowerToUpperRule(inputSchema);
        case "regex_replace":
            return applyRegexReplaceRule(inputSchema, ruleConfig.loadConfig(RegexReplaceRule.class));
        case "truncate":
            return applyTruncateRule(inputSchema, ruleConfig.loadConfig(TruncateRule.class));
        case "upper_to_lower":
            return applyUpperToLowerRule(inputSchema);
        case "unique_number_suffix":
            return applyUniqueNumberSuffixRule(inputSchema, ruleConfig.loadConfig(UniqueNumberSuffixRule.class));
        default:
            throw new ConfigException("Renaming rule \"" +rule+ "\" is unknown");
        }
    }

    private Schema applyCharacterTypesRule(Schema inputSchema, CharacterTypesRule rule) {
        final List<String> passTypes = rule.getPassTypes();
        final String passCharacters = rule.getPassCharacters();
        final String replace = rule.getReplace();

        if (replace.isEmpty()) {
            throw new ConfigException("\"replace\" in \"character_types\" must not be explicitly empty");
        }
        if (replace.length() != 1) {
            throw new ConfigException("\"replace\" in \"character_types\" must contain just 1 character");
        }
        // TODO(dmikurube): Revisit this for better escaping.
        if (passCharacters.contains("\\E")) {
            throw new ConfigException("\"pass_characters\" in \"character_types\" must not contain \"\\E\"");
        }

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

    private Schema applyFirstCharacterTypesRule(Schema inputSchema, FirstCharacterTypesRule rule) {
        final Optional<String> replace = rule.getReplace();
        final List<String> passTypes = rule.getPassTypes();
        final String passCharacters = rule.getPassCharacters();
        final Optional<String> prefix = rule.getPrefix();

        if (replace.isPresent() && replace.get().length() != 1) {
            throw new ConfigException("\"replace\" in \"first_character_types\" must contain just 1 character if specified");
        }
        if (prefix.isPresent() && prefix.get().length() != 1) {
            throw new ConfigException("\"prefix\" in \"first_character_types\" must contain just 1 character if specified");
        }
        if (prefix.isPresent() && replace.isPresent()) {
            throw new ConfigException("\"replace\" and \"prefix\" in \"first_character_types\" must not be specified together");
        }
        if ((!prefix.isPresent()) && (!replace.isPresent())) {
            throw new ConfigException("Either of \"replace\" or \"prefix\" must be specified in \"first_character_types\"");
        }
        // TODO(dmikurube): Revisit this for better escaping.
        if (passCharacters.contains("\\E")) {
            throw new ConfigException("\"pass_characters\" in \"first_character_types\" must not contain \"\\E\"");
        }

        StringBuilder regexBuilder = new StringBuilder();
        regexBuilder.append("^[^");
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
        regexBuilder.append("].*");

        Schema.Builder schemaBuidler = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            String name = column.getName();
            if (name.matches(regexBuilder.toString())) {
                if (replace.isPresent()) {
                    name = replace.get() + name.substring(1);
                }
                else if (prefix.isPresent()) {
                    name = prefix.get() + name;
                }
            }
            schemaBuidler.add(name, column.getType());
        }
        return schemaBuidler.build();
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
            if (column.getName().length() <= rule.getMaxLength()) {
                builder.add(column.getName(), column.getType());
            }
            else {
                try {
                    builder.add(column.getName().substring(0, rule.getMaxLength()), column.getType());
                }
                catch (IndexOutOfBoundsException ex) {
                    logger.error("FATAL unexpected error in \"truncate\" rule: substring failed.");
                    throw new AssertionError("FATAL unexpected error in \"truncate\" rule: substring failed.", ex);
                }
            }
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

    private Schema applyRegexReplaceRule(Schema inputSchema, RegexReplaceRule rule) {
        final String match = rule.getMatch();
        final String replace = rule.getReplace();

        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            // TODO(dmikurube): Check if we need a kind of sanitization?
            try {
                builder.add(column.getName().replaceAll(match, replace), column.getType());
            }
            catch (PatternSyntaxException ex) {
                throw new ConfigException(ex);
            }
        }
        return builder.build();
    }

    /**
     * Resolves conflicting column names by suffixing numbers.
     *
     * Conflicts are resolved by the following rules. The rules should not be changed casually because changing the
     * rules breaks compatibility.
     *
     * 1. Count all duplicates in the original column names. Indexes are counted up per original column name.
     * 2. Fix new column names from the left to the right
     *   - Try to append the current index for the original column name (with truncation if requested (not implemented))
     *     - Fix the new name if no duplication is found with fixed column names on the left and original column names
     *     - Retry with an index incremented if a duplication is found with fixed column names on the left
     *
     * Examples:
     *     [c, c1, c1,   c2, c,   c3]
     * ==> [c, c1, c1_2, c2, c_2, c3]
     *
     * If a newly suffixed name newly conflicts with other columns, the index is just skipped. For example:
     *     [c, c,   c_0, c_1, c_2]
     * ==> [c, c_3, c_0, c_1, c_2]
     *
     * If truncation is requested simultaneously with uniqueness (not implemented), it should work like:
     *     [co, c, co  , c  , co  , c  , ..., co  , c  , co  , c   , co  , c   ]
     * ==> [co, c, co_2, c_2, co_3, c_3, ..., co_9, c_9, c_10, c_11, c_12, c_13] (max_length:4)
     *
     *     [co, co  , co  , ..., co  , c, c  , ..., c  , co  , c  , co  , c  , co  , c   ]
     * ==> [co, co_2, co_3, ..., co_9, c, c_2, ..., c_7, c_10, c_8, c_11, c_9, c_12, c_13] (max_length:4)
     *
     * Note that a delimiter should not be omitted. Recurring conflicts may confuse users.
     *     [c, c,  c,  ..., c,   c,   c,   c,   c1, c1,  c1]
     * NG: [c, c2, c3, ..., c10, c11, c12, c13, c1, c12, c13] (not unique!)
     * ==> [c, c2, c3, ..., c10, c11, c12, c13, c1, c14, c15] (confusing)
     */
    private Schema applyUniqueNumberSuffixRule(Schema inputSchema, UniqueNumberSuffixRule rule) {
        final String delimiter = rule.getDelimiter();
        final Optional<Integer> digits = rule.getDigits();
        final Optional<Integer> maxLength = rule.getMaxLength();
        final int offset = rule.getOffset();

        // |delimiter| must consist of just 1 character to check quickly that it does not contain any digit.
        if (delimiter == null || delimiter.length() != 1 || Character.isDigit(delimiter.charAt(0))) {
            throw new ConfigException("\"delimiter\" in rule \"unique_number_suffix\" must contain just 1 non-digit character");
        }
        if (maxLength.isPresent() && maxLength.get() < minimumMaxLengthInUniqueNumberSuffix) {
            throw new ConfigException("\"max_length\" in rule \"unique_number_suffix\" must be larger than " +(minimumMaxLengthInUniqueNumberSuffix-1));
        }
        if (maxLength.isPresent() && digits.isPresent() && maxLength.get() < digits.get() + delimiter.length()) {
            throw new ConfigException("\"max_length\" in rule \"unique_number_suffix\" must be larger than \"digits\"");
        }
        int digitsOfNumberOfColumns = Integer.toString(inputSchema.getColumnCount() + offset - 1).length();
        if (maxLength.isPresent() && maxLength.get() <= digitsOfNumberOfColumns) {
            throw new ConfigException("\"max_length\" in rule \"unique_number_suffix\" must be larger than digits of ((number of columns) + \"offset\" - 1)");
        }
        if (digits.isPresent() && digits.get() <= digitsOfNumberOfColumns) {
            throw new ConfigException("\"digits\" in rule \"unique_number_suffix\" must be larger than digits of ((number of columns) + \"offset\" - 1)");
        }

        // Columns should not be truncated here initially. Uniqueness should be identified before truncated.

        // Iterate for initial states.
        HashSet<String> originalColumnNames = new HashSet<>();
        HashMap<String, Integer> columnNameCountups = new HashMap<>();
        for (Column column : inputSchema.getColumns()) {
            originalColumnNames.add(column.getName());
            columnNameCountups.put(column.getName(), offset);
        }

        Schema.Builder outputBuilder = Schema.builder();

        HashSet<String> fixedColumnNames = new HashSet<>();
        for (Column column : inputSchema.getColumns()) {
            String truncatedName = column.getName();
            if (column.getName().length() > maxLength.or(Integer.MAX_VALUE)) {
                truncatedName = column.getName().substring(0, maxLength.get());
            }

            // Fix with the new name candidate if the new name does not conflict with the fixed names on the left.
            // Conflicts with original names do not matter here.
            if (!fixedColumnNames.contains(truncatedName)) {
                // The original name is counted up.
                columnNameCountups.put(column.getName(), columnNameCountups.get(column.getName()) + 1);
                // The truncated name is fixed.
                fixedColumnNames.add(truncatedName);
                outputBuilder.add(truncatedName, column.getType());
                continue;
            }

            int index = columnNameCountups.get(column.getName());
            String concatenatedName;
            do {
                // This can be replaced with String#format(Locale.ENGLISH, ...), but Java's String#format does not
                // have variable widths ("%*d" in C's printf). It cannot be very simple with String#format.
                String differentiatorString = Integer.toString(index);
                if (digits.isPresent() && (digits.get() > differentiatorString.length())) {
                    differentiatorString =
                        Strings.repeat("0", digits.get() - differentiatorString.length()) + differentiatorString;
                }
                differentiatorString = delimiter + differentiatorString;
                concatenatedName = column.getName() + differentiatorString;
                if (concatenatedName.length() > maxLength.or(Integer.MAX_VALUE)) {
                    concatenatedName =
                        column.getName().substring(0, maxLength.get() - differentiatorString.length())
                        + differentiatorString;
                }
                ++index;
            // Conflicts with original names matter when creating new names with suffixes.
            } while (fixedColumnNames.contains(concatenatedName) || originalColumnNames.contains(concatenatedName));
            // The original name is counted up.
            columnNameCountups.put(column.getName(), index);
            // The concatenated&truncated name is fixed.
            fixedColumnNames.add(concatenatedName);
            outputBuilder.add(concatenatedName, column.getType());
        }
        return outputBuilder.build();
    }

    private static final ImmutableMap<String, String> CHARACTER_TYPE_KEYWORDS =
        new ImmutableMap.Builder<String, String>().put("a-z", "a-z")
                                                  .put("A-Z", "A-Z")
                                                  .put("0-9", "0-9")
                                                  .build();

    // TODO(dmikurube): Revisit the limitation.
    // It should be practically acceptable to assume any output accepts column names with 8 characters at least...
    private static final int minimumMaxLengthInUniqueNumberSuffix = 8;

    private final Logger logger = Exec.getLogger(getClass());
}
