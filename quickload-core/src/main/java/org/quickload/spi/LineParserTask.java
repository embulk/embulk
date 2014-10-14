package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Config;
import org.quickload.config.TaskSource;

public interface LineParserTask
        extends ParserTask
{
    // TODO encoding, malformed input reporting behvior, etc.
    //@Config("encoding", default = "utf8")
    //public String getEncoding();
}
