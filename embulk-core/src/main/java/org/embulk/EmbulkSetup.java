package org.embulk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.embulk.config.ConfigSource;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

/**
 * EmbulkSetup initiates an EmbulkRunner instance. It was originally implemented with Ruby in lib/embulk.rb.
 *
 * NOTE: Developers should not depend on this EmbulkSetup class. This class is created tentatively, and may be
 * re-implemented again in a different style.
 */
public class EmbulkSetup
{
    @Deprecated
    public static EmbulkRunner setupWithNewScriptingContainer(final Map<String, Object> systemConfigGiven)
    {
        // The JRuby instance is a global singleton so that the settings here affects later execution.
        // The local variable should be persistent so that local variables are set through ScriptingContainer.put.
        final ScriptingContainer globalJRubyContainer =
            new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.PERSISTENT);
        return setup(systemConfigGiven, globalJRubyContainer);
    }

    public static EmbulkRunner setup(
            final Map<String, Object> systemConfigGiven,
            final ScriptingContainer globalJRubyContainer)
    {
        // NOTE: When it was in Ruby "require 'json'" was required to format the system config into a JSON string.

        // NOTE: require_classpath is called only when Embulk is loaded as a Ruby gem. (lib/embulk.rb)

        final HashMap<String, Object> systemConfigModified = new HashMap<String, Object>(systemConfigGiven);

        // use the global ruby runtime for all ScriptingContainer
        // injected by org.embulk.jruby.JRubyScriptingModule
        systemConfigModified.put("use_global_ruby_runtime", true);

        // Calling ObjectMapper simply just as a formatter here so that it does not depend much on Jackson.
        // Do not leak the Jackson object outside.
        final ObjectMapper jacksonObjectMapper = new ObjectMapper();
        final String systemConfigJson;
        try {
            systemConfigJson = jacksonObjectMapper.writeValueAsString(systemConfigModified);
        }
        catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

        final EmbulkEmbed.Bootstrap bootstrap = new org.embulk.EmbulkEmbed.Bootstrap();
        final ConfigSource systemConfig = bootstrap.getSystemConfigLoader().fromJsonString(systemConfigJson);
        bootstrap.setSystemConfig(systemConfig);
        final EmbulkEmbed embed = bootstrap.initialize();  // see embulk-core/src/main/java/org/embulk/jruby/JRubyScriptingModule.

        // see also embulk/java/bootstrap.rb loaded by JRubyScriptingModule
        globalJRubyContainer.runScriptlet("module Embulk; end");
        globalJRubyContainer.put("__internal_embulk_setup_embed__", embed);
        globalJRubyContainer.put("__internal_embulk_setup_global_jruby_container__", globalJRubyContainer);
        globalJRubyContainer.runScriptlet("Embulk.const_set :Runner, Embulk::EmbulkRunner.new(__internal_embulk_setup_embed__)");
        globalJRubyContainer.remove("__internal_embulk_setup_global_jruby_container__");
        globalJRubyContainer.remove("__internal_embulk_setup_embed__");

        return new EmbulkRunner(embed, globalJRubyContainer);
    }
}
