package org.embulk.config;

import java.util.Properties;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import com.google.inject.Inject;
import org.embulk.spi.Exec;
import org.embulk.EmbulkTestRuntime;

public class TestConfigLoader
{
    private ConfigLoader loader;

    @Before
    public void setup() throws Exception
    {
        this.loader = new ConfigLoader(new ModelManager(null, new ObjectMapper()));
    }

    @Test
    public void testFromEmptyJson() throws IOException
    {
        ConfigSource config = loader.fromJson(newInputStream("{\"type\":\"test\",\"data\":1}"));
        assertEquals("test", config.get(String.class, "type"));
        assertEquals(1, (int) config.get(Integer.class, "data"));
    }

    @Test
    public void testFromYamlProperties() throws IOException
    {
        Properties props = new Properties();
        props.setProperty("type", "test");
        props.setProperty("data", "1");

        ConfigSource config = loader.fromPropertiesYamlLiteral(props, "");
        assertEquals("test", config.get(String.class, "type"));
        assertEquals(1, (int) config.get(Integer.class, "data"));
    }

    @Test
    public void testFromYamlPropertiesNested() throws IOException
    {
        Properties props = new Properties();
        props.setProperty("type", "test");
        props.setProperty("columns.k1", "1");
        props.setProperty("values.myval.data", "2");

        ConfigSource config = loader.fromPropertiesYamlLiteral(props, "");
        System.out.println("config: "+config);
        assertEquals("test", config.get(String.class, "type"));
        assertEquals(1, (int) config.getNested("columns").get(Integer.class, "k1"));
        assertEquals(2, (int) config.getNested("values").getNested("myval").get(Integer.class, "data"));
    }

    private static InputStream newInputStream(String string)
    {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }
}
