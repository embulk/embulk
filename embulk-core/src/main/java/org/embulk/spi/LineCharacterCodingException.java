package org.embulk.spi;

import java.nio.charset.CharacterCodingException;

public class LineCharacterCodingException
        extends RuntimeException
{
    public LineCharacterCodingException(CharacterCodingException cause)
    {
        super(cause);
    }

    @Override
    public CharacterCodingException getCause()
    {
        return (CharacterCodingException) super.getCause();
    }
}
