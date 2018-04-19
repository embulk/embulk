package org.embulk.plugin.jar;

public class ExampleJarSpiV0 {
    public String getTestString() {
        return "foobar";
    }

    public ExampleDependencyJar getDependencyObject() {
        return new ExampleDependencyJar();
    }
}
