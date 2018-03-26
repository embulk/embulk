package org.embulk;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.Map;
import java.util.Random;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.ModelManager;
import org.embulk.exec.ExecModule;
import org.embulk.exec.ExtensionServiceLoaderModule;
import org.embulk.exec.SystemConfigModule;
import org.embulk.jruby.JRubyScriptingModule;
import org.embulk.plugin.BuiltinPluginSourceModule;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginClassLoaderModule;
import org.embulk.plugin.PluginType;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecSession;
import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterPlugin;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class EmbulkTestRuntime extends GuiceBinder {
    private static ConfigSource getSystemConfig() {
        // TODO set some default values
        return new DataSourceImpl(null);
    }

    public static class TestRuntimeModule implements Module {
        @Override
        public void configure(Binder binder) {
            ConfigSource systemConfig = getSystemConfig();
            new SystemConfigModule(systemConfig).configure(binder);
            new ExecModule().configure(binder);
            new ExtensionServiceLoaderModule(systemConfig).configure(binder);
            new BuiltinPluginSourceModule().configure(binder);
            new JRubyScriptingModule(systemConfig).configure(binder);
            new PluginClassLoaderModule(systemConfig).configure(binder);
            new TestUtilityModule().configure(binder);
            new TestPluginSourceModule().configure(binder);
        }
    }

    private ExecSession exec;

    public EmbulkTestRuntime() {
        super(new TestRuntimeModule());
        Injector injector = getInjector();
        ConfigSource execConfig = new DataSourceImpl(injector.getInstance(ModelManager.class));
        this.exec = ExecSession.builder(injector).fromExecConfig(execConfig).build();
    }

    public ExecSession getExec() {
        return exec;
    }

    public BufferAllocator getBufferAllocator() {
        return getInstance(BufferAllocator.class);
    }

    public ModelManager getModelManager() {
        return getInstance(ModelManager.class);
    }

    public Random getRandom() {
        return getInstance(RandomManager.class).getRandom();
    }

    public PluginClassLoaderFactory getPluginClassLoaderFactory() {
        return getInstance(PluginClassLoaderFactory.class);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        final Statement superStatement = EmbulkTestRuntime.super.apply(base, description);
        return new Statement() {
            public void evaluate() throws Throwable {
                try {
                    Exec.doWith(exec, new ExecAction<Void>() {
                            public Void run() {
                                exec.setReporters(createReporters());
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

    private static Map<Reporter.Channel, Reporter> createReporters() {
        final ImmutableMap.Builder<Reporter.Channel, Reporter> builder = ImmutableMap.builder();
        for (final Reporter.Channel channel : Reporter.Channel.values()) {
            builder.put(channel, createStdoutReporter());
        }
        return Maps.immutableEnumMap(builder.build());
    }

    private static Reporter createStdoutReporter() {
        final ReporterPlugin plugin = Exec.newPlugin(ReporterPlugin.class, PluginType.STDOUT);
        return plugin.open(Exec.newTaskSource());
    }

    private static class RuntimeExecutionException extends RuntimeException {
        public RuntimeExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
