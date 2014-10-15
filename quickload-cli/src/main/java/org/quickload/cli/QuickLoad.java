package org.quickload.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.quickload.config.ConfigSource;
import org.quickload.config.ModelManager;
import org.quickload.exec.ExecModule;
import org.quickload.exec.ExtensionServiceLoaderModule;
import org.quickload.exec.LocalExecutor;
import org.quickload.plugin.BuiltinPluginSourceModule;

public class QuickLoad {
    public static void main(String[] args) throws Exception
    {
        JsonNodeFactory js = JsonNodeFactory.instance;
        ObjectNode json = js.objectNode();

        ObjectNode inputType = js.objectNode();
        inputType.put("injected", "my");
        json.put("in:type", inputType);

        ArrayNode inPaths = js.arrayNode();
        inPaths.add("/tmp/csv_01.csv");
        inPaths.add("/tmp/csv_02.csv");
        json.put("in:paths", inPaths);

        ArrayNode schema = js.arrayNode();
        schema.add(column(js, 0, "date_code", "string"));
        schema.add(column(js, 1, "customer_code", "long"));
        schema.add(column(js, 2, "product_code", "string"));
        schema.add(column(js, 3, "employee_code", "string"));
        json.put("in:schema", schema);

        ObjectNode parserType = js.objectNode();
        parserType.put("injected", "my");
        json.put("in:parser_type", parserType);

        ObjectNode outputType = js.objectNode();
        outputType.put("injected", "my");
        json.put("out:type", outputType);

        ObjectNode formatterType = js.objectNode();
        formatterType.put("injected", "my");
        json.put("out:formatter_type", formatterType);

        json.put("out:compress_type", "none");

        ArrayNode outPaths = js.arrayNode();
        outPaths.add("/tmp/output_csv_01.csv");
        outPaths.add("/tmp/output_csv_02.csv");
        json.put("out:paths", outPaths);

        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new ExecModule());
        modules.add(new ExtensionServiceLoaderModule());
        modules.add(new BuiltinPluginSourceModule());
        modules.add(new MyPluginModule()); // TODO
        Injector injector = Guice.createInjector(modules.build());

        ModelManager modelManager = injector.getInstance(ModelManager.class);
        ConfigSource config = new ConfigSource(modelManager, json);

        try (LocalExecutor exec = injector.getInstance(LocalExecutor.class)) {
            exec.configure(config);
            exec.begin();
            exec.start();
            exec.join();
        }
    }

    private static ObjectNode column(JsonNodeFactory js,
            int index, String name, String type)
    {
        ObjectNode column = js.objectNode();
        column.put("index", 0);
        column.put("name", name);
        column.put("type", type);
        return column;
    }
}
