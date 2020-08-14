package org.embulk.deps.guess;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class TestCharsetDetectorImpl {
    @Test
    public void testDefault() {
        final CharsetDetectorImpl detector = new CharsetDetectorImpl();
        detector.setText("あいうえお".getBytes(StandardCharsets.UTF_8));
        final CharsetMatchImpl match = detector.detect();
        assertEquals("UTF-8", match.getName());
    }
}
