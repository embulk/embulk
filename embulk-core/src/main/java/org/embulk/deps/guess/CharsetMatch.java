package org.embulk.deps.guess;

@Deprecated
public class CharsetMatch {
    private CharsetMatch() {
        // No direct instantiation.
    }

    @Deprecated
    public int getConfidence() {
        return 50;
    }

    @Deprecated
    public String getName() {
        return "UTF-8";
    }

    static final CharsetMatch INSTANCE = new CharsetMatch();
}
