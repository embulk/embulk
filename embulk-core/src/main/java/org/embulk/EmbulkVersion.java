package org.embulk;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public final class EmbulkVersion
{
    private EmbulkVersion()
    {
    }

    static {
        final Properties properties = new Properties();
        String versionLoaded = null;
        try (InputStream input = EmbulkVersion.class.getClassLoader().getResourceAsStream("embulk.properties")) {
            properties.load(input);
            versionLoaded = properties.getProperty("version");
        }
        catch (IOException ex) {
            versionLoaded = "(embulk-java-properties-not-found)";
        }
        VERSION = versionLoaded;
    }

    public static final String VERSION;
}
