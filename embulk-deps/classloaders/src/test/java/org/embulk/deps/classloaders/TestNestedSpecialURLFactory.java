package org.embulk.deps.classloaders;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import org.junit.Test;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class TestNestedSpecialURLFactory {
    @Test
    public void testSimple() throws Exception {
        final NestedSpecialURLFactory factory =
                NestedSpecialURLFactory.ofFileURL(new URL("file:///path/to/top.jar"), "spproto");
        final URL url = factory.create("/path2/to/contained.jar", "/path3/to/resource.file");
        assertEquals("spproto:jar:file:/path/to/top.jar!/path2/to/contained.jar!/path3/to/resource.file",
                     url.toString());
        assertEquals("jar:file:/path/to/top.jar!/path2/to/contained.jar!/path3/to/resource.file", url.getFile());
        assertEquals("", url.getHost());
        assertEquals("jar:file:/path/to/top.jar!/path2/to/contained.jar!/path3/to/resource.file", url.getPath());
        assertEquals(-1, url.getPort());
        assertEquals("spproto", url.getProtocol());
        assertEquals(null, url.getQuery());
        assertEquals(null, url.getRef());
        assertEquals(null, url.getUserInfo());
    }
}
