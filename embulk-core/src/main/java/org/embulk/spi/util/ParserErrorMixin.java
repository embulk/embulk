package org.embulk.spi.util;

import java.util.List;
import com.google.common.base.Optional;
import org.msgpack.value.Value;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskReport;
import org.embulk.plugin.PluginType;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.TransactionalValueOutput;
import org.embulk.spi.ErrorPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Exec;
import org.embulk.spi.Mixin;
import org.embulk.spi.MixinId;
import org.embulk.spi.FileInput;

public class ParserErrorMixin
        implements Mixin<ParserPlugin>
{
    private Wrapper wrapper;

    public static ParserErrorMixin empty()
    {
        // TODO return something meaningful
        return null;
    }

    @Override
    public ParserPlugin mixin(ParserPlugin plugin)
    {
        this.wrapper = new Wrapper(plugin);
        return wrapper;
    }

    public void add(Value record)
    {
        TransactionalValueOutput out = wrapper.errorOutput;
        if (out == null) {
            throw new IllegalStateException("ParserErrorMixin.error is available only at task context");
        }
        out.add(record);
    }

    private static class Wrapper
            implements ParserPlugin
    {
        public static interface WrapperTask
                extends Task
        {
            @Config("error")
            @ConfigDefault("null")
            public Optional<ConfigSource> getErrorConfig();
        }

        private final ParserPlugin plugin;
        private TransactionalValueOutput errorOutput;

        public Wrapper(ParserPlugin plugin)
        {
            this.plugin = plugin;
        }

        public void transaction(ConfigSource config, final ParserPlugin.Control control)
        {
            WrapperTask task = config.loadConfig(WrapperTask.class);
            final ConfigSource errorConfig = task.getErrorConfig().or(
                    Exec.newConfigSource().set("type", "warn")  // TODO configurable default error plugin type
                    );
            final PluginType errorType = errorConfig.get(PluginType.class, "type");
            final ErrorPlugin error = Exec.newPlugin(ErrorPlugin.class, errorType);

            plugin.transaction(config, new ParserPlugin.Control() {
                public void run(final TaskSource pluginTaskSource, final Schema pluginSchema)
                {
                    error.transaction(errorConfig, new ErrorPlugin.Control() {
                        public List<TaskReport> run(TaskSource errorTaskSource)
                        {
                            MixinId mixinId = Exec.newMixinId();
                            TaskSource taskSource = Exec.newTaskSource()
                                .set("PluginTaskSource", pluginTaskSource)
                                .set("ErrorPluginType", errorType)
                                .set("ErrorTaskSource", errorTaskSource)
                                .set("MixinId", mixinId);
                            control.run(taskSource, pluginSchema);
                            return Exec.getMixinReports(mixinId);
                        }
                    });
                }
            });
        }

        public void run(TaskSource taskSource, Schema schema,
                FileInput input, PageOutput output)
        {
            TaskSource pluginTaskSource = taskSource.get(TaskSource.class, "PluginTaskSource");
            PluginType errorType = taskSource.get(PluginType.class, "ErrorPluginType");
            TaskSource errorTaskSource = taskSource.get(TaskSource.class, "ErrorTaskSource");
            ErrorPlugin error = Exec.newPlugin(ErrorPlugin.class, errorType);
            MixinId mixinId = taskSource.get(MixinId.class, "MixinId");

            TransactionalValueOutput tran = errorOutput = error.open(errorTaskSource);
            try {
                plugin.run(pluginTaskSource, schema, input, output);
                TaskReport report = tran.commit();
                tran = null;
                Exec.reportMixinTask(mixinId, report);
            }
            finally {
                try {
                    if (tran != null) {
                        tran.abort();
                    }
                }
                finally {
                    errorOutput.close();
                    errorOutput = null;
                }
            }
        }
    }
}
