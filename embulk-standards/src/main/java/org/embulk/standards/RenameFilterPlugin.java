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

import com.google.common.collect.ImmutableMap;

import java.util.List;
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
        List<Object> getRulesList();
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema,
                            FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        Map<String, String> renameMap = task.getRenameMap();
        List<Object> rulesList = task.getRulesList();

        // check column_options is valid or not
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
        for (Object rule : rulesList) {
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

    private Schema applyRule(Object ruleObject, Schema inputSchema) throws ConfigException
    {
        String rule;
        Map<String, Object> parameters;

        if (ruleObject instanceof String) {
            @SuppressWarnings("unchecked") String ruleString = (String)ruleObject;
            rule = ruleString;
            parameters = ImmutableMap.<String, Object>of();
        } else if (ruleObject instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> ruleMap = (Map)ruleObject;
            if ((!ruleMap.containsKey("rule")) ||
                (!(ruleMap.get("rule") instanceof String))) {
                throw new ConfigException("Renaming operator in map is not a string.");
            }
            @SuppressWarnings("unchecked") String ruleString = (String)(ruleMap.get("rule"));
            rule = ruleString;
            parameters = ImmutableMap.<String, Object>copyOf(ruleMap);
        } else {
            throw new ConfigException("Renaming operator is not a string.");
        }

        switch (rule) {
        case "convert_lower_case_to_upper":
            return applyConvertLowerCaseToUpper(inputSchema, parameters);
        case "convert_upper_case_to_lower":
            return applyConvertUpperCaseToLower(inputSchema, parameters);
        default:
            throw new ConfigException("Renaming operator \"" +rule+ "\" is unknown");
        }
    }

    private Schema applyConvertUpperCaseToLower(Schema inputSchema, Map<String, Object> parameters) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().toLowerCase(), column.getType());
        }
        return builder.build();
    }

    private Schema applyConvertLowerCaseToUpper(Schema inputSchema, Map<String, Object> parameters) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().toUpperCase(), column.getType());
        }
        return builder.build();
    }
}
