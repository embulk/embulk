package org.embulk.jruby;

import com.google.inject.Inject;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.InvokeFailedException;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.ExecutorPlugin;

public class JRubyPluginSource
        implements PluginSource
{
    private final ScriptingContainer jruby;
    private final Object rubyPluginManager;

    @Inject
    public JRubyPluginSource(ScriptingContainer jruby)
    {
        this.jruby = jruby;

        // get Embulk::Plugin
        //this.rubyPluginManager = ((RubyModule) jruby.get("Embulk")).const_get(
        //        RubySymbol.newSymbol(
        //            jruby.getProvider().getRuntime(), "Plugin"));
        this.rubyPluginManager = jruby.runScriptlet("Embulk::Plugin");
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException
    {
        String name = type.getName();

        String category;
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
            throw new PluginSourceNotMatchException("Plugin interface "+iface+" is not supported in JRuby");
        }

        String methodName = "new_java_" + category;
        try {
            return jruby.callMethod(rubyPluginManager, methodName, name, iface);
        } catch (InvokeFailedException ex) {
            throw new PluginSourceNotMatchException(ex.getCause());
        }
    }
}
