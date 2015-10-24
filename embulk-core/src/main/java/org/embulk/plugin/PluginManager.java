package org.embulk.plugin;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.embulk.config.ConfigException;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PluginMixin;
import org.embulk.spi.Mixin;
import org.embulk.plugin.compat.PluginWrappers;

public class PluginManager
{
    private final List<PluginSource> sources;
    private final Injector injector;

    // Set<PluginSource> is injected by BuiltinPluginSourceModule or extensions
    // using Multibinder<PluginSource>.
    @Inject
    public PluginManager(Set<PluginSource> pluginSources, Injector injector)
    {
        this.sources = ImmutableList.copyOf(pluginSources);
        this.injector = injector;
    }

    @SuppressWarnings("unchecked")
    public <T> T newPlugin(Class<T> iface, PluginType type)
    {
        T plugin = newPluginWithoutWrapper(iface, type);
        T wrapped = plugin;
        if (plugin instanceof InputPlugin) {
            wrapped = (T) PluginWrappers.inputPlugin((InputPlugin) plugin);
        }
        wrapped = mixin(iface, plugin, wrapped);
        return wrapped;
    }

    private <T> T newPluginWithoutWrapper(Class<T> iface, PluginType type)
    {
        if (sources.isEmpty()) {
            throw new ConfigException("No PluginSource is installed");
        }

        if (type == null) {
            throw new ConfigException(String.format("%s type is not set (if you intend to use NullOutputPlugin, you should enclose null in quotes such as {type: \"null\"}.", iface.getSimpleName()));
        }

        List<PluginSourceNotMatchException> exceptions = new ArrayList<>();
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, type);
            }
            catch (PluginSourceNotMatchException e) {
                exceptions.add(e);
            }
        }

        throw buildPluginNotFoundException(iface, type, exceptions);
    }

    private <T> T mixin(Class<T> iface, T plugin, T target)
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Field targetField : plugin.getClass().getDeclaredFields()) {
            if (targetField.getAnnotation(PluginMixin.class) != null) {
                Class<?> mixinClass = targetField.getType();
                if (!Mixin.class.isAssignableFrom(mixinClass)) {
                    throw new NullPointerException(String.format(
                                "Field %s.%s with @PluginMixin must be subclass of Mixin<%s>",
                                plugin.getClass().getName(), targetField.getName(), iface.getSimpleName()));
                }
                Method mixinMethod;
                try {
                    mixinMethod = mixinClass.getMethod("mixin", iface);
                }
                catch (NoSuchMethodException | SecurityException ex) {
                    throw new NullPointerException(String.format(
                                "Field %s.%s with @PluginMixin must be subclass of Mixin<%s>",
                                plugin.getClass().getName(), targetField.getName(), iface.getSimpleName()));
                }

                targetField.setAccessible(true);

                try {
                    target = mixinInstance(mixinClass, lookup.unreflect(mixinMethod),
                            plugin, lookup.unreflectSetter(targetField),
                            target);
                }
                catch (Throwable ex) {
                    throw Throwables.propagate(ex);
                }
            }
        }

        for (Method setterMethod : plugin.getClass().getDeclaredMethods()) {
            if (setterMethod.getAnnotation(PluginMixin.class) != null) {
                Class<?>[] args = setterMethod.getParameterTypes();
                if (args.length != 1) {
                    throw new NullPointerException(String.format(
                                "Method %s.%s with @PluginMixin must have exactly one argument",
                                plugin.getClass().getName(), setterMethod.getName()));
                }
                Class<?> mixinClass = args[0];
                if (!Mixin.class.isAssignableFrom(mixinClass)) {
                    throw new NullPointerException(String.format(
                                "Argument of method %s.%s with @PluginMixin must be subclass of Mixin<%s>",
                                plugin.getClass().getName(), setterMethod.getName(), iface.getSimpleName()));
                }
                Method mixinMethod;
                try {
                    mixinMethod = mixinClass.getMethod("mixin", iface);
                }
                catch (NoSuchMethodException | SecurityException ex) {
                    throw new NullPointerException(String.format(
                                "Argument of method %s.%s with @PluginMixin must be subclass of Mixin<%s>",
                                plugin.getClass().getName(), setterMethod.getName(), iface.getSimpleName()));
                }

                setterMethod.setAccessible(true);

                try {
                    target = mixinInstance(mixinClass, lookup.unreflect(mixinMethod),
                            plugin, lookup.unreflect(setterMethod),
                            target);
                }
                catch (Throwable ex) {
                    throw Throwables.propagate(ex);
                }
            }
        }

        return target;
    }

    private <T> T mixinInstance(Class<?> mixinClass, MethodHandle mixinMethod,
            T plugin, MethodHandle pluginSetter, T mixinTarget) throws Throwable
    {
        Object mixin = injector.getInstance(mixinClass);
        T result = (T) mixinMethod.invoke(mixin, mixinTarget);
        pluginSetter.invoke(plugin, mixin);
        return result;
    }

    private static ConfigException buildPluginNotFoundException(Class<?> iface, PluginType type,
            List<PluginSourceNotMatchException> exceptions)
    {
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
}
