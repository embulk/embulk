package org.embulk.spi.unit;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Deprecated
public class ToStringMap extends HashMap<String, String> {
    @SuppressWarnings("deprecation")
    public ToStringMap(Map<String, ToString> map) {
        super(mapToStringString(map));
    }

    private ToStringMap(final Map<String, String> map, final boolean placeHolder) {
        super(map);
    }

    public static ToStringMap of(final Map<String, String> map) {
        return new ToStringMap(map, true);
    }

    public Properties toProperties() {
        Properties props = new Properties();
        props.putAll(this);
        return props;
    }

    private static Map<String, String> mapToStringString(final Map<String, ToString> mapOfToString) {
        final HashMap<String, String> result = new HashMap<>();
        for (final Map.Entry<String, ToString> entry : mapOfToString.entrySet()) {
            final ToString value = entry.getValue();
            if (value == null) {
                result.put(entry.getKey(), null);
            } else {
                result.put(entry.getKey(), value.toString());
            }
        }
        return result;
    }
}
