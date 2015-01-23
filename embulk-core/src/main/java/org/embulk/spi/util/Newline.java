package org.embulk.spi.util;

public enum Newline
{
    CRLF("\r\n"),
    LF("\n"),
    CR("\r");

    private final String string;
    private final char firstCharCode;
    private final char secondCharCode;

    private Newline(String string)
    {
        this.string = string;
        this.firstCharCode = string.charAt(0);
        if (string.length() > 1) {
            this.secondCharCode = string.charAt(1);
        } else {
            this.secondCharCode = 0;
        }
    }

    public String getString()
    {
        return string;
    }

    public char getFirstCharCode()
    {
        return firstCharCode;
    }

    public char getSecondCharCode()
    {
        return secondCharCode;
    }
}
