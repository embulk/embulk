package org.quickload.cli;

import org.quickload.config.ConfigSource;
import org.quickload.exec.LocalExecutor;

public class QuickLoad {
    public static void main(String[] args) throws Exception
    {
        try (LocalExecutor exec = new LocalExecutor()) {
            exec.configure(new ConfigSource());
            exec.begin();
            exec.start();
            exec.join();
        }
    }
}
