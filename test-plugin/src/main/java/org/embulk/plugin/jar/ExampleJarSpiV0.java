package org.embulk.plugin.jar;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ExampleJarSpiV0 implements Callable<String>, Supplier<ExampleDependencyJar> {
    @Override
    public String call() {
        return "foobar";
    }

    @Override
    public ExampleDependencyJar get() {
        return new ExampleDependencyJar();
    }
}
