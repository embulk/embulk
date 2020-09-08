package org.embulk.spi.util;

import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FileInput;

/**
 * Utility class for handling multiple decoder plugins.
 *
 * <p>It is considered to be an internal class, not for plugins. To make it explicit, {@link EncodersInternal} replaces it.
 */
@Deprecated
public abstract class Decoders {
    private Decoders() {}

    public static List<DecoderPlugin> newDecoderPlugins(ExecSession exec, List<ConfigSource> configs) {
        if (!(exec instanceof ExecSessionInternal)) {
            throw new IllegalArgumentException(new ClassCastException());
        }
        final ExecSessionInternal execInternal = (ExecSessionInternal) exec;

        return DecodersInternal.newDecoderPlugins(execInternal, configs);
    }

    public interface Control extends DecodersInternal.Control {
        @Override
        public void run(List<TaskSource> taskSources);
    }

    public static void transaction(List<DecoderPlugin> plugins, List<ConfigSource> configs,
            Decoders.Control control) {
        DecodersInternal.transaction(plugins, configs, control);
    }

    public static FileInput open(List<DecoderPlugin> plugins, List<TaskSource> taskSources,
            FileInput input) {
        return DecodersInternal.open(plugins, taskSources, input);
    }
}
