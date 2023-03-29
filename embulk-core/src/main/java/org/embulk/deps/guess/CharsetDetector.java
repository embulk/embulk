package org.embulk.deps.guess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class CharsetDetector {
    private CharsetDetector() {
        // No direct instantiation.
    }

    @Deprecated
    public static CharsetDetector create() {
        logger.warn(
                "Class org.embulk.deps.guess.CharsetDetector is no longer available. "
                + "It always returns \"UTF-8\" unconditionally. Use an appropriate guess plugin explicitly.");
        return new CharsetDetector();
    }

    @Deprecated
    public CharsetDetector setText(final byte[] in) {
        return this;
    }

    @Deprecated
    public CharsetMatch detect() {
        return CharsetMatch.INSTANCE;
    }

    private static final Logger logger = LoggerFactory.getLogger(CharsetDetector.class);
}
