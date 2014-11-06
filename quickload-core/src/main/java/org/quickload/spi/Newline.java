package org.quickload.spi;

// TODO this enum is not used yet becase Jackson needs custom serde to
//      deal with enum. LineDecoderTask.getNewline should use this enum.
public enum Newline
{
    CRLF("\r\n"),
    LF("\n"),
    CR("\r");

    private final String code;

    private Newline(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }
}
