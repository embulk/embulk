package org.embulk.deps.guess;

import com.ibm.icu.text.CharsetDetector;

public final class CharsetDetectorImpl extends org.embulk.deps.guess.CharsetDetector {
    public CharsetDetectorImpl() {
        this.detector = new CharsetDetector();
    }

    @Override
    public CharsetDetectorImpl setText(final byte[] in) {
        this.detector.setText(in);
        return this;
    }

    @Override
    public CharsetMatchImpl detect() {
        return new CharsetMatchImpl(this.detector.detect());
    }

    private final CharsetDetector detector;
}
