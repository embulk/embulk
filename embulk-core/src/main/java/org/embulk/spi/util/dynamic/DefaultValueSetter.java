package org.embulk.spi.util.dynamic;

import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.time.Timestamp;

public interface DefaultValueSetter
{
    void setBoolean(PageBuilder pageBuilder, Column c);

    void setLong(PageBuilder pageBuilder, Column c);

    void setDouble(PageBuilder pageBuilder, Column c);

    void setString(PageBuilder pageBuilder, Column c);

    void setTimestamp(PageBuilder pageBuilder, Column c);

    void setJson(PageBuilder pageBuilder, Column c);
}
