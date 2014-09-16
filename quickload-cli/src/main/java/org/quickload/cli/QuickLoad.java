package org.quickload.cli;

import com.google.common.collect.ImmutableMap;
import org.quickload.config.ConfigSource;
import org.quickload.config.ModelManager;
import org.quickload.exec.LocalExecutor;

public class QuickLoad {
    public static void main(String[] args) throws Exception
    {
        ModelManager modelManager = new ModelManager();

        ImmutableMap.Builder<String,String> builder = ImmutableMap.builder();
        builder.put("paths", "[\"a\",\"b\"]");
        ConfigSource config = new ConfigSource(modelManager, builder.build());

        try (LocalExecutor exec = new LocalExecutor(modelManager)) {
            exec.configure(config);
            exec.begin();
            exec.start();
            exec.join();
        }
    }
}
