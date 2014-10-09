package org.quickload.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.quickload.config.ConfigSource;
import org.quickload.config.ModelManager;
import org.quickload.exec.ExecModule;
import org.quickload.exec.ExtensionServiceLoaderModule;
import org.quickload.exec.LocalExecutor;
import org.quickload.plugin.BuiltinPluginSourceModule;

public class QuickLoad {
    public static void main(String[] args) throws Exception
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("in:paths", "[\"/tmp/csv_01.csv\",\"/tmp/csv_02.csv\"]");
        builder.put("in:schema", "{\"columns\":[{\"index\":0,\"name\":\"date_code\",\"type\":\"string\"},{\"index\":1,\"name\":\"customer_code\",\"type\":\"long\"},{\"index\":2,\"name\":\"product_code\",\"type\":\"string\"},{\"index\":3,\"name\":\"employee_code\",\"type\":\"string\"}]}");
        builder.put("ConfigExpression", "\"{\\\"name\\\":\\\"my\\\"}\"");
        builder.put("out:paths", "[\"/tmp/output_csv_01.csv\",\"/tmp/output_csv_02.csv\"]");

        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new ExecModule());
        modules.add(new ExtensionServiceLoaderModule());
        modules.add(new BuiltinPluginSourceModule());
        modules.add(new MyPluginSettingsModule()); // TODO
        Injector injector = Guice.createInjector(modules.build());

        ModelManager modelManager = injector.getInstance(ModelManager.class);
        ConfigSource config = new ConfigSource(modelManager, builder.build());

        try (LocalExecutor exec = injector.getInstance(LocalExecutor.class)) {
            exec.configure(config);
            exec.begin();
            exec.start();
            exec.join();
        }
    }
}
