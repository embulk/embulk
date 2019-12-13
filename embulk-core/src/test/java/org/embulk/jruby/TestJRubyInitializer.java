package org.embulk.jruby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TestJRubyInitializer {
    @Test
    public void testArguments() {
        final JRubyInitializer initializer = JRubyInitializer.of(
                null,
                LoggerFactory.getLogger(TestJRubyInitializer.class),
                "/gem/home",
                null,
                true,
                "/load/path" + java.io.File.pathSeparator + "/another/path",
                "/classpath" + java.io.File.pathSeparator + "/2nd/classpath",
                "--option,arg",
                "/bundle",
                false);

        // TODO: Test through mocked ScriptingContainerDelegate, not through probing methods for testing.

        assertEquals("/gem/home", initializer.probeGemHomeForTesting());
        assertTrue(initializer.probeUseDefaultEmbulkGemHomeForTesting());

        assertEquals(null, initializer.probeGemPathForTesting());

        assertEquals(2, initializer.probeJRubyLoadPathForTesting().size());
        assertEquals("/load/path", initializer.probeJRubyLoadPathForTesting().get(0));
        assertEquals("/another/path", initializer.probeJRubyLoadPathForTesting().get(1));

        assertEquals(2, initializer.probeJRubyClasspathForTesting().size());
        assertEquals("/classpath", initializer.probeJRubyClasspathForTesting().get(0));
        assertEquals("/2nd/classpath", initializer.probeJRubyClasspathForTesting().get(1));

        assertEquals(2, initializer.probeJRubyOptionsForTesting().size());
        assertEquals("--option", initializer.probeJRubyOptionsForTesting().get(0));
        assertEquals("arg", initializer.probeJRubyOptionsForTesting().get(1));

        assertEquals("/bundle", initializer.probeJRubyBundlerPluginSourceDirectoryForTesting());
    }
}
