package org.embulk.spi;

import java.util.List;
import com.google.inject.Module;
import org.embulk.config.ConfigSource;

/**
 * Extension is a module to extend the execution framework using Guice.
 * Unlike plugins, extensions can overwrite or add core components such as
 * BufferManager, PluginSource, etc.
 * Extension is not designed for users but for framework developers to make
 * core components loosely coupled.
 *
 * An example extension to add a custom PluginSource will be as following:
 *
 * <code>
 * class MyPluginSourceExtension
 *         implements Extension, Module
 * {
 *     public static class MyPluginSource
 *             implements PluginSource
 *     {
 *         // ...
 *     }
 *
 *     {@literal @}Override
 *     public void configure(Binder binder)
 *     {
 *         Multibinder&lt;PluginSource&gt; multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
 *         multibinder.addBinding().to(MyPluginSource.class);
 *     }
 *
 *     {@literal @}Override
 *     public List&lt;Module&gt; getModules()
 *     {
 *         return ImmutableList.&lt;Module&gt;of(this);
 *     }
 * }
 * </code>
 */
public interface Extension
{
    List<Module> getModules(ConfigSource systemConfig);
}
