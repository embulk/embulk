package org.embulk.plugin;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.ConfigException;
import org.embulk.jruby.JRubyPluginSource;
import org.embulk.plugin.InjectedPluginSource;
import org.embulk.plugin.maven.MavenPluginSource;

public class PluginManager {
    private PluginManager(
            final InjectedPluginSource injectedSource,
            final MavenPluginSource mavenSource,
            final JRubyPluginSource jrubySource) {
        this.injectedSource = injectedSource;
        this.mavenSource = mavenSource;
        this.jrubySource = jrubySource;
    }

    public static PluginManager with(
            final InjectedPluginSource injectedSource,
            final MavenPluginSource mavenSource,
            final JRubyPluginSource jrubySource) {
        return new PluginManager(
                injectedSource,
                mavenSource,
                jrubySource);
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) {
        if (type == null) {
            throw new ConfigException(String.format(
                    "%s type is not set (if you intend to use NullOutputPlugin, you should enclose null in quotes such as {type: \"null\"}.",
                    iface.getSimpleName()));
        }

        List<PluginSourceNotMatchException> exceptions = new ArrayList<>();

        try {
            return this.injectedSource.newPlugin(iface, type);
        } catch (final PluginSourceNotMatchException e) {
            exceptions.add(e);
        }

        try {
            return this.mavenSource.newPlugin(iface, type);
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

    private final InjectedPluginSource injectedSource;
    private final MavenPluginSource mavenSource;
    private final JRubyPluginSource jrubySource;
}
