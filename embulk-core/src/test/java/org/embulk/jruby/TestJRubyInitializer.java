package org.embulk.jruby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TestJRubyInitializer {
    @Test
    public void testArguments() {
        final List<String> loadPath = new ArrayList<>();
        loadPath.add("/load/path");
        final List<String> classpath = new ArrayList<>();
        classpath.add("/classpath");
        final List<String> options = new ArrayList<>();
        options.add("--option");
        options.add("arg");

        final JRubyInitializer initializer = JRubyInitializer.of(
                null,
                LoggerFactory.getLogger(TestJRubyInitializer.class),
                "/gem/home",
                true,
                loadPath,
                classpath,
                options,
                "/bundle");

        // TODO: Test through mocked ScriptingContainerDelegate, not through probing methods for testing.

        assertEquals("/gem/home", initializer.probeGemHomeForTesting());
        assertTrue(initializer.probeUseDefaultEmbulkGemHomeForTesting());

        assertEquals(1, initializer.probeJRubyLoadPathForTesting().size());
        assertEquals("/load/path", initializer.probeJRubyLoadPathForTesting().get(0));

        assertEquals(1, initializer.probeJRubyClasspathForTesting().size());
        assertEquals("/classpath", initializer.probeJRubyClasspathForTesting().get(0));

        assertEquals(2, initializer.probeJRubyOptionsForTesting().size());
        assertEquals("--option", initializer.probeJRubyOptionsForTesting().get(0));
        assertEquals("arg", initializer.probeJRubyOptionsForTesting().get(1));

        assertEquals("/bundle", initializer.probeJRubyBundlerPluginSourceDirectoryForTesting());
    }
}
