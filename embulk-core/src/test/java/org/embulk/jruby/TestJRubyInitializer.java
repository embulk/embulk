package org.embulk.jruby;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import org.embulk.EmbulkSystemProperties;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TestJRubyInitializer {
    @Test
    public void testArguments() {
        final Properties embulkSystemProperties = new Properties();

        embulkSystemProperties.setProperty("gem_home", "/gem/home");
        embulkSystemProperties.setProperty("jruby_load_path", "/load/path" + java.io.File.pathSeparator + "/another/path");
        embulkSystemProperties.setProperty("jruby_classpath", "/classpath" + java.io.File.pathSeparator + "/2nd/classpath");
        embulkSystemProperties.setProperty("jruby_command_line_options", "--option,arg");
        embulkSystemProperties.setProperty("jruby_global_bundler_plugin_source_directory", "/bundle");
        embulkSystemProperties.setProperty("jruby.require.sigdump", "false");

        final JRubyInitializer initializer = JRubyInitializer.of(
                null,
                LoggerFactory.getLogger(TestJRubyInitializer.class),
                EmbulkSystemProperties.of(embulkSystemProperties));

        // TODO: Test through mocked ScriptingContainerDelegate, not through probing methods for testing.

        assertEquals("/gem/home", initializer.probeGemHomeForTesting());

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
