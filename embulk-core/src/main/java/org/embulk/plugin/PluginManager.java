package org.embulk.plugin;

import java.util.ArrayList;
import java.util.List;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.ConfigException;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.LocalExecutorPlugin;
import org.embulk.exec.SamplingParserPlugin;
import org.embulk.jruby.JRubyPluginSource;
import org.embulk.plugin.maven.MavenPluginSource;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.ParserPlugin;

public class PluginManager {
    private PluginManager(
            final EmbulkSystemProperties embulkSystemProperties,
            final BuiltinPluginSource builtinSource,
            final MavenPluginSource mavenSource,
            final SelfContainedPluginSource selfContainedSource,
            final JRubyPluginSource jrubySource) {
        this.embulkSystemProperties = embulkSystemProperties;
        this.builtinSource = builtinSource;
        this.mavenSource = mavenSource;
        this.selfContainedSource = selfContainedSource;
        this.jrubySource = jrubySource;
    }

    public static PluginManager with(
            final EmbulkSystemProperties embulkSystemProperties,
            final BuiltinPluginSource builtinSource,
            final MavenPluginSource mavenSource,
            final SelfContainedPluginSource selfContainedSource,
            final JRubyPluginSource jrubySource) {
        return new PluginManager(
                embulkSystemProperties,
                builtinSource,
                mavenSource,
                selfContainedSource,
                jrubySource);
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) {
        if (type == null) {
            throw new ConfigException(String.format(
                    "%s type is not set (if you intend to use NullOutputPlugin, you should enclose null in quotes such as {type: \"null\"}.",
                    iface.getSimpleName()));
        }

        List<PluginSourceNotMatchException> exceptions = new ArrayList<>();

        // GuessExecutor
        if (ParserPlugin.class.equals(iface) && "system_guess".equals(type.getName())) {
            return iface.cast(new GuessExecutor.GuessParserPlugin());
        }

        // PreviewExecutor
        if (ParserPlugin.class.equals(iface) && "system_sampling".equals(type.getName())) {
            return iface.cast(new SamplingParserPlugin());
        }

        // LocalExecutorPlugin
        if (ExecutorPlugin.class.equals(iface) && "local".equals(type.getName())) {
            return iface.cast(new LocalExecutorPlugin(this.embulkSystemProperties));
        }

        // The order is intentional.
        // * BuiltinPluginSource comes first because "built-in" ones are there always much intentionally (e.g. for testing).
        // * MavenPluginSource comes second so that newly-installed Maven-based plugins can override self-contained ones.
        // * JRubyPluginSource comes last because JRuby is optional, and RubyGem-based plugins are the last choice.

        try {
            return this.builtinSource.newPlugin(iface, type);
        } catch (final PluginSourceNotMatchException e) {
            exceptions.add(e);
        }

        try {
            return this.mavenSource.newPlugin(iface, type);
        } catch (final PluginSourceNotMatchException e) {
            exceptions.add(e);
        }

        try {
            return this.selfContainedSource.newPlugin(iface, type);
        } catch (final PluginSourceNotMatchException e) {
            exceptions.add(e);
        }

        try {
            return this.jrubySource.newPlugin(iface, type);
        } catch (final PluginSourceNotMatchException e) {
            exceptions.add(e);
        }

        throw buildPluginNotFoundException(iface, type, exceptions);
    }

    private static ConfigException buildPluginNotFoundException(
            Class<?> iface, PluginType type, List<PluginSourceNotMatchException> exceptions) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("%s '%s' is not found.", iface.getSimpleName(), type.getName()));
        for (PluginSourceNotMatchException exception : exceptions) {
            Throwable cause = (exception.getCause() == null ? exception : exception.getCause());
            if (cause.getMessage() != null) {
                message.append(String.format("%n"));
                message.append(cause.getMessage());
            }
        }
        ConfigException e = new ConfigException(message.toString());
        for (PluginSourceNotMatchException exception : exceptions) {
            e.addSuppressed(exception);
        }
        return e;
    }

    private final EmbulkSystemProperties embulkSystemProperties;
    private final BuiltinPluginSource builtinSource;
    private final MavenPluginSource mavenSource;
    private final SelfContainedPluginSource selfContainedSource;
    private final JRubyPluginSource jrubySource;
}
