package org.embulk.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ModelManager;
import org.jruby.embed.ScriptingContainer;

public class Runner
        extends EmbulkService
{
    public static void main(String[] args)
    {
        // TODO bootstrap model manager
        ConfigSource systemConfig = new ConfigLoader(new ModelManager(null, new ObjectMapper())).fromPropertiesYamlLiteral(System.getProperties(), "embulk.");

        new Runner(systemConfig).run(args);
    }

    public Runner(ConfigSource systemConfig)
    {
        super(systemConfig);
    }

    public void run(String[] args)
    {
        ScriptingContainer jruby = injector.getInstance(ScriptingContainer.class);  // injected by org.embulk.jruby.JRubyScriptingModule
        jruby.callMethod(jruby.runScriptlet("ARGV"), "replace", (Object) args);
        jruby.runScriptlet("require 'embulk/command/embulk'");
    }
}
