package org.embulk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.embulk.config.ConfigSource;

/**
 * EmbulkSetup initiates an EmbulkRunner instance. It was originally implemented with Ruby in lib/embulk.rb.
 *
 * NOTE: Developers should not depend on this EmbulkSetup class. This class is created tentatively, and may be
 * re-implemented again in a different style.
 */
public class EmbulkSetup
{
    public static EmbulkRunner setup(final Map<String, Object> systemConfigGiven)
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

        return new EmbulkRunner(embed);
    }
}
