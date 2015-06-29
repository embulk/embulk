package org.embulk.spi;

import java.io.IOException;

public class TempFileException
        extends RuntimeException
{
    public TempFileException(IOException cause)
    {
        super(cause);
    }


    @Override
    public IOException getCause()
    {
        return (IOException) super.getCause();
    }
}
