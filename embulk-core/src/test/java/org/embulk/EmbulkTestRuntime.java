package org.embulk;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.Properties;
import java.util.Random;
import org.embulk.EmbulkEmbed;
import org.embulk.config.ModelManager;
import org.embulk.exec.ExecModule;
import org.embulk.exec.ExtensionServiceLoaderModule;
import org.embulk.exec.SystemConfigModule;
import org.embulk.jruby.JRubyScriptingModule;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginClassLoaderFactoryImpl;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecInternal;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.MockFormatterPlugin;
import org.embulk.spi.MockParserPlugin;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class EmbulkTestRuntime extends GuiceBinder {
    public static class TestRuntimeModule implements Module {
        @Override
        public void configure(Binder binder) {
            final EmbulkSystemProperties embulkSystemProperties = EmbulkSystemProperties.of(new Properties());
            new SystemConfigModule(embulkSystemProperties).configure(binder);
            new ExecModule(embulkSystemProperties).configure(binder);
            new ExtensionServiceLoaderModule(embulkSystemProperties).configure(binder);
            new JRubyScriptingModule().configure(binder);
            new TestUtilityModule().configure(binder);
        }
    }

    private ExecSessionInternal exec;

    public EmbulkTestRuntime() {
        super(new TestRuntimeModule());
        Injector injector = getInjector();
        final ModelManager model = createModelManager();
        this.exec = ExecSessionInternal.builderInternal(injector)
                .setModelManager(model)
                .registerParserPlugin("mock", MockParserPlugin.class)
                .registerFormatterPlugin("mock", MockFormatterPlugin.class)
                .build();
    }

    public ExecSessionInternal getExec() {
        return exec;
    }

    public BufferAllocator getBufferAllocator() {
        return getInstance(BufferAllocator.class);
    }

    @SuppressWarnings("deprecation")
    public ModelManager getModelManager() {
        return this.exec.getModelManager();
    }

    public Random getRandom() {
        return getInstance(RandomManager.class).getRandom();
    }

    public static PluginClassLoaderFactory buildPluginClassLoaderFactory() {
        return PluginClassLoaderFactoryImpl.of(EmbulkEmbed.PARENT_FIRST_PACKAGES, EmbulkEmbed.PARENT_FIRST_RESOURCES);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        final Statement superStatement = EmbulkTestRuntime.super.apply(base, description);
        return new Statement() {
            public void evaluate() throws Throwable {
                try {
                    ExecInternal.doWith(exec, new ExecAction<Void>() {
                            public Void run() {
                                try {
                                    superStatement.evaluate();
                                } catch (Throwable ex) {
                                    throw new RuntimeExecutionException(ex);
                                }
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
