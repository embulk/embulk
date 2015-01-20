package org.embulk;

import static org.embulk.plugin.InjectedPluginSource.registerPlugin;

import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.MockFormatterPlugin;
import org.embulk.spi.MockParserPlugin;
import org.embulk.spi.ParserPlugin;

import com.google.inject.Binder;
import com.google.inject.Module;

public class TestPluginSourceModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        registerPlugin(binder, ParserPlugin.class, "mock",
                MockParserPlugin.class);
        registerPlugin(binder, FormatterPlugin.class, "mock",
                MockFormatterPlugin.class);
    }
}
