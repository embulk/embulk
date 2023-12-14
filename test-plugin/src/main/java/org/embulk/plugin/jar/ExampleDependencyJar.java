package org.embulk.plugin.jar;

import java.util.concurrent.Callable;

public class ExampleDependencyJar implements Callable<String> {
    @Override
    public String call() {
        return "hoge";
    }
}
