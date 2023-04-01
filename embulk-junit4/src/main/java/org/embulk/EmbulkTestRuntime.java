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
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class EmbulkTestRuntime implements TestRule {
    private ExecSessionInternal exec;
    private final RandomManager random;

    public EmbulkTestRuntime() {
        this.random = new RandomManager();
    }

    private void reset() {
        this.exec = null;
    }

    public ExecSessionInternal getExec() {
        if (this.exec == null) {
            final ModelManager model = createModelManager();
            this.exec = ExecSessionInternal
                    .builderInternal(PooledBufferAllocator.create(), new SimpleTempFileSpaceAllocator())
                    .setModelManager(model)
                    .registerParserPlugin("mock", MockParserPlugin.class)
                    .registerFormatterPlugin("mock", MockFormatterPlugin.class)
                    .build();
        }
        return this.exec;
    }

    public BufferAllocator getBufferAllocator() {
        return this.getExec().getBufferAllocator();
    }

    @SuppressWarnings("deprecation")
    public ModelManager getModelManager() {
        return this.getExec().getModelManager();
    }

    public Random getRandom() {
        return this.random.getRandom();
    }

    public static PluginClassLoaderFactory buildPluginClassLoaderFactory() {
        return PluginClassLoaderFactoryImpl.forTesting(EmbulkEmbed.PARENT_FIRST_PACKAGES, EmbulkEmbed.PARENT_FIRST_RESOURCES);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        final Statement statement = new EmbulkTestWatcher().apply(base, description);
        return new Statement() {
            public void evaluate() throws Throwable {
                try {
                    ExecInternal.doWith(getExec(), new ExecAction<Void>() {
                            public Void run() {
                                try {
                                    statement.evaluate();
                                } catch (final Throwable ex) {
                                    throw new RuntimeExecutionException(ex);
                                }
                                return null;
                            }
                        });
                } catch (RuntimeException ex) {
                    throw ex.getCause();
                } finally {
                    getExec().cleanup();
                }
            }
        };
    }

    private class EmbulkTestWatcher extends TestWatcher {
        @Override
        protected void starting(final Description description) {
            reset();
        }
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
