package org.quickload.jruby;

import com.google.inject.Inject;
import com.fasterxml.jackson.databind.JsonNode;
import org.jruby.RubySymbol;
import org.jruby.RubyModule;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.InvokeFailedException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.quickload.plugin.PluginSource;
import org.quickload.plugin.PluginSourceNotMatchException;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.FileDecoderPlugin;
import org.quickload.spi.FileEncoderPlugin;
import org.quickload.spi.LineFilterPlugin;
import org.quickload.spi.ParserGuessPlugin;

public class JRubyPluginSource
        implements PluginSource
{
    private final ScriptingContainer jruby;
    private final Object rubyPluginManager;

    @Inject
    public JRubyPluginSource(ScriptingContainer jruby)
    {
        this.jruby = jruby;

        // get QuickLoad::Plugin
        //this.rubyPluginManager = ((RubyModule) jruby.get("QuickLoad")).const_get(
        //        RubySymbol.newSymbol(
        //            jruby.getProvider().getRuntime(), "Plugin"));
        this.rubyPluginManager = jruby.runScriptlet("QuickLoad::Plugin");
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig) throws PluginSourceNotMatchException
    {
        if (!typeConfig.isTextual()) {
            throw new PluginSourceNotMatchException();
        }
        String name = typeConfig.asText();

        String category;
        if (InputPlugin.class.equals(iface)) {
            category = "input";
        } else if (OutputPlugin.class.equals(iface)) {
            category = "output";
        } else if (ParserPlugin.class.equals(iface)) {
            category = "parser";
        } else if (FormatterPlugin.class.equals(iface)) {
            category = "formatter";
        } else if (FileDecoderPlugin.class.equals(iface)) {
            category = "decoder";
        } else if (FileEncoderPlugin.class.equals(iface)) {
            category = "encoder";
        } else if (LineFilterPlugin.class.equals(iface)) {
            category = "line_filter";
        } else if (ParserGuessPlugin.class.equals(iface)) {
            category = "guess";
        } else {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface "+iface+" is not supported in JRuby");
        }

        String methodName = "new_" + category;
        try {
            return jruby.callMethod(rubyPluginManager, methodName, name, iface);
        } catch (InvokeFailedException ex) {
            throw new PluginSourceNotMatchException(ex.getCause());
        }
    }
}
