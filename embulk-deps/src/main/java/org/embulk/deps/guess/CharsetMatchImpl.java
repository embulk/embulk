package org.embulk.deps.guess;

import com.ibm.icu.text.CharsetMatch;

public final class CharsetMatchImpl extends org.embulk.deps.guess.CharsetMatch {
    CharsetMatchImpl(final CharsetMatch match) {
        this.match = match;
    }

    @Override
    public int getConfidence() {
        return this.match.getConfidence();
    }

    @Override
    public String getName() {
        return this.match.getName();
    }

    private final CharsetMatch match;
}
