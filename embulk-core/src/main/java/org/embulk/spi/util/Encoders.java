package org.embulk.spi.util;

import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FileOutput;

/**
 * Utility class for handling multiple encoder plugins.
 *
 * <p>It is considered to be an internal class, not for plugins. To make it explicit, {@link EncodersInternal} replaces it.
 */
@Deprecated
public abstract class Encoders {
    private Encoders() {}

    public static List<EncoderPlugin> newEncoderPlugins(ExecSession exec, List<ConfigSource> configs) {
        if (!(exec instanceof ExecSessionInternal)) {
            throw new IllegalArgumentException(new ClassCastException());
        }
        final ExecSessionInternal execInternal = (ExecSessionInternal) exec;

        return EncodersInternal.newEncoderPlugins(execInternal, configs);
    }

    public interface Control extends EncodersInternal.Control {
        public void run(List<TaskSource> taskSources);
    }

    public static void transaction(List<EncoderPlugin> plugins, List<ConfigSource> configs,
            Encoders.Control control) {
        EncodersInternal.transaction(plugins, configs, control);
    }

    public static FileOutput open(List<EncoderPlugin> plugins, List<TaskSource> taskSources,
            FileOutput output) {
        return EncodersInternal.open(plugins, taskSources, output);
    }
}
