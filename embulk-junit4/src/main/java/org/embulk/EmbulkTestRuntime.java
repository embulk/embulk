package org.embulk;

import java.util.Random;
import org.embulk.EmbulkEmbed;
import org.embulk.config.ModelManager;
import org.embulk.deps.buffer.PooledBufferAllocator;
import org.embulk.exec.SimpleTempFileSpaceAllocator;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginClassLoaderFactoryImpl;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecInternal;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.MockFormatterPlugin;
import org.embulk.spi.MockParserPlugin;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class EmbulkTestRuntime implements TestRule {
    private ExecSessionInternal exec;
    private final RandomManager random;

    public EmbulkTestRuntime() {
        final ModelManager model = createModelManager();
        this.exec = ExecSessionInternal
                .builderInternal(PooledBufferAllocator.create(), new SimpleTempFileSpaceAllocator())
                .setModelManager(model)
                .registerParserPlugin("mock", MockParserPlugin.class)
                .registerFormatterPlugin("mock", MockFormatterPlugin.class)
                .build();
        this.random = new RandomManager();
    }

    public ExecSessionInternal getExec() {
        return exec;
    }

    public BufferAllocator getBufferAllocator() {
        return this.exec.getBufferAllocator();
    }

    @SuppressWarnings("deprecation")
    public ModelManager getModelManager() {
        return this.exec.getModelManager();
    }

    public Random getRandom() {
        return this.random.getRandom();
    }

    public static PluginClassLoaderFactory buildPluginClassLoaderFactory() {
        return PluginClassLoaderFactoryImpl.forTesting(EmbulkEmbed.PARENT_FIRST_PACKAGES, EmbulkEmbed.PARENT_FIRST_RESOURCES);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                try {
                    ExecInternal.doWith(exec, new ExecAction<Void>() {
                            public Void run() {
                                return null;
                            }
                        });
                } catch (RuntimeException ex) {
                    throw ex.getCause();
                } finally {
                    exec.cleanup();
                }
            }
        };
    }

    private static class RuntimeExecutionException extends RuntimeException {
        public RuntimeExecutionException(Throwable cause) {
            super(cause);
        }
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private static org.embulk.config.ModelManager createModelManager() {
        return new org.embulk.config.ModelManager();
    }
}
