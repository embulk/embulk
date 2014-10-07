package org.quickload.cli;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Injector;
import org.quickload.config.ConfigSource;
import org.quickload.config.ModelManager;
import org.quickload.exec.LocalExecutor;
import org.quickload.exec.ExecModule;
import org.quickload.record.TypeManager;

public class QuickLoad {
    public static void main(String[] args) throws Exception
    {
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new ExecModule());

        // TODO inject XxxManager
        Injector injector = Guice.createInjector(modules.build());

        ModelManager modelManager = new ModelManager(); // TODO should inject
        TypeManager typeManager = new TypeManager(modelManager); // TODO should inject

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("in:paths", "[\"/tmp/csv_01.csv\",\"/tmp/csv_02.csv\"]");
        builder.put("out:paths", "[\"/tmp/output_csv_01.csv\",\"/tmp/output_csv_02.csv\"]");
        builder.put("schema", "{\"columns\":[{\"index\":0,\"name\":\"date_code\",\"type\":\"string\"},{\"index\":1,\"name\":\"customer_code\",\"type\":\"long\"},{\"index\":2,\"name\":\"product_code\",\"type\":\"string\"},{\"index\":3,\"name\":\"employee_code\",\"type\":\"string\"}]}");
        ConfigSource config = new ConfigSource(modelManager, builder.build());

        // TODO how can we load plugins?

        //LocalExecutor exec = injector.getInstance(LocalExecutor.class)
        try (LocalExecutor exec = new LocalExecutor(modelManager)) {
            exec.configure(config);
            exec.begin();
            exec.start();
            exec.join();
        }
    }
}
