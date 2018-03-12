package org.embulk.jruby;

import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class JRubyPluginSource implements PluginSource {
    private final ScriptingContainerDelegate jruby;

    public JRubyPluginSource(ScriptingContainerDelegate jruby) {
        this.jruby = jruby;
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException {
        if (type.getSourceType() != PluginSource.Type.DEFAULT) {
            throw new PluginSourceNotMatchException();
        }

        final String name = type.getName();

        final String category;
        if (InputPlugin.class.isAssignableFrom(iface)) {
            category = "input";
        } else if (OutputPlugin.class.isAssignableFrom(iface)) {
            category = "output";
        } else if (ParserPlugin.class.isAssignableFrom(iface)) {
            category = "parser";
        } else if (FormatterPlugin.class.isAssignableFrom(iface)) {
            category = "formatter";
        } else if (DecoderPlugin.class.isAssignableFrom(iface)) {
            category = "decoder";
        } else if (EncoderPlugin.class.isAssignableFrom(iface)) {
            category = "encoder";
        } else if (FilterPlugin.class.isAssignableFrom(iface)) {
            category = "filter";
        } else if (GuessPlugin.class.isAssignableFrom(iface)) {
            category = "guess";
        } else if (ExecutorPlugin.class.isAssignableFrom(iface)) {
            category = "executor";
        } else {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface " + iface + " is not supported in JRuby");
        }

        String methodName = "new_java_" + category;
        try {
            // get Embulk::Plugin
            //this.rubyPluginManager = ((RubyModule) jruby.get("Embulk")).const_get(
            //        RubySymbol.newSymbol(
            //            jruby.getProvider().getRuntime(), "Plugin"));
            final Object rubyPluginManager = jruby.runScriptlet("Embulk::Plugin");
            return jruby.callMethod(rubyPluginManager, methodName, name, iface);
        } catch (Throwable ex) {
            throw new PluginSourceNotMatchException(ex);
        }
    }
}
