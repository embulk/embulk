package org.quickload.cli;

import java.util.List;
import java.io.File;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.quickload.config.ConfigSource;
import org.quickload.config.ConfigSources;
import org.quickload.config.NextConfig;
import org.quickload.exec.ExecModule;
import org.quickload.exec.ExtensionServiceLoaderModule;
import org.quickload.exec.LocalExecutor;
import org.quickload.exec.GuessExecutor;
import org.quickload.plugin.BuiltinPluginSourceModule;
import org.quickload.jruby.JRubyScriptingModule;
import org.quickload.standards.StandardPluginModule;

public class QuickLoad {
    public static void main(String[] args) throws Exception
    {
        new QuickLoad().run(args);
    }

    public void run(final String[] args) throws Exception
    {
        if (args.length == 0) {
            System.out.println("usage: [-Dload.systemConfigKey=value...] <config.yml> [configKey=value...]");
            return;
        }

        ConfigSource systemConfig = ConfigSources.fromPropertiesYamlLiteral(System.getProperties(), "load.");

        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new ExecModule());
        modules.add(new ExtensionServiceLoaderModule());
        modules.add(new BuiltinPluginSourceModule());
        modules.add(new StandardPluginModule());
        modules.addAll(getAdditionalModules());

        Injector injector = Guice.createInjector(modules.build());

        File configPath = new File(args[0]);
        ConfigSource config = ConfigSources.fromYamlFile(configPath);

        // TODO
        //NextConfig guessed = injector.getInstance(GuessExecutor.class).run(config);
        //System.out.println("guessed: "+guessed);

        LocalExecutor exec = injector.getInstance(LocalExecutor.class);
        NextConfig nextConfig = exec.run(config);

        System.out.println("next config: "+nextConfig);
    }

    protected Iterable<? extends Module> getAdditionalModules()
    {
        ImmutableList.Builder<Module> builder = ImmutableList.builder();
        builder.add(new ExecModule());
        builder.add(new ExtensionServiceLoaderModule());
        builder.add(new BuiltinPluginSourceModule());
        builder.add(new StandardPluginModule());
        builder.add(new JRubyScriptingModule());
        return builder.build();
    }
}
