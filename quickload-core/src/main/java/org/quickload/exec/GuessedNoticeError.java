package org.quickload.exec;

import org.quickload.config.NextConfig;

public class GuessedNoticeError
        extends Error
{
    private final NextConfig guessed;

    public GuessedNoticeError(NextConfig guessed)
    {
        this.guessed = guessed;
    }

    public NextConfig getGuessed()
    {
        return guessed;
    }
}
