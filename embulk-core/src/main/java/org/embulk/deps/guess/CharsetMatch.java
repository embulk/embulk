package org.embulk.deps.guess;

public abstract class CharsetMatch {
    public abstract int getConfidence();

    public abstract String getName();
}
