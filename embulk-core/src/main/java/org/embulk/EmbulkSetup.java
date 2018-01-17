package org.embulk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.embulk.config.ConfigSource;

/**
 * EmbulkSetup initiates an EmbulkRunner instance. It was originally implemented with Ruby in lib/embulk.rb.
 *
 * NOTE: Developers should not depend on this EmbulkSetup class. This class is created tentatively, and may be
 * re-implemented again in a different style.
 */
public class EmbulkSetup {
    public static EmbulkRunner setup(final Map<String, Object> systemConfigGiven) {
        final HashMap<String, Object> systemConfigModified = new HashMap<String, Object>(systemConfigGiven);

        // Calling ObjectMapper simply just as a formatter here so that it does not depend much on Jackson.
        // Do not leak the Jackson object outside.
        final ObjectMapper jacksonObjectMapper = new ObjectMapper();
        final String systemConfigJson;
        try {
            systemConfigJson = jacksonObjectMapper.writeValueAsString(systemConfigModified);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

        final EmbulkEmbed.Bootstrap bootstrap = new org.embulk.EmbulkEmbed.Bootstrap();
        final ConfigSource systemConfig = bootstrap.getSystemConfigLoader().fromJsonString(systemConfigJson);
        bootstrap.setSystemConfig(systemConfig);
        final EmbulkEmbed embed = bootstrap.initialize();  // see embulk-core/src/main/java/org/embulk/jruby/JRubyScriptingModule.

        return new EmbulkRunner(embed);
    }
}
