package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Tests org.embulk.spi.time.RubyTimeFormat for formatter.
 */
public class TestRubyTimeFormatForFormatter {
    @Test
    public void testEmpty() {
        testFormat("");
    }

    private void testFormat(final String formatString, final Object... expectedTokensInArray) {
        final List<RubyTimeFormatToken> expectedTokens = new ArrayList<>();
        for (final Object expectedElement : expectedTokensInArray) {
            if (expectedElement instanceof RubyTimeFormatToken) {
                expectedTokens.add((RubyTimeFormatToken) expectedElement);
            } else if (expectedElement instanceof List) {
                for (final Object expectedElement2 : (List) expectedElement) {
                    if (expectedElement2 instanceof RubyTimeFormatToken) {
                        expectedTokens.add((RubyTimeFormatToken) expectedElement2);
                    } else {
                        fail();
                    }
                }
            } else {
                fail();
            }
        }
        final RubyTimeFormat expectedFormat = RubyTimeFormat.createForTesting(expectedTokens);
        final RubyTimeFormat actualFormat = RubyTimeFormat.compileForFormatter(formatString);
        assertEquals(expectedFormat, actualFormat);
    }
}
