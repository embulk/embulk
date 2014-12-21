package org.embulk.exec;

import java.util.List;
import com.google.inject.Module;

/**
 * Extension is a module to extend the execution framework using Guice.
 * Unlike plugins, extensions can overwrite or add core components such as
 * BufferManager, PluginSource, etc.
 * Extension is not designed for users but for framework developpers to make
 * core components loosely coupled.
 *
 * An example extention to add a custom PluginSource will be as following:
 *
 * class MyPluginSourceExtension
 *         implements Extension, Module
 * {
 *     public static class MyPluginSource
 *             implements PluginSource
 *     {
 *         // ...
 *     }
 *
 *     @Override
 *     public void configure(Binder binder)
 *     {
 *         Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
 *         multibinder.addBinding().to(MyPluginSource.class);
 *     }
 *
 *     @Override
 *     public List<Module> getModules()
 *     {
 *         return ImmutableList.<Module>of(this);
 *     }
 * }
 */
public interface Extension
{
    public List<Module> getModules();
}
