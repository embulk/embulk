package org.embulk.spi.util.dynamic;

import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.time.Timestamp;

public class NullDefaultValueSetter
        implements DefaultValueSetter
{
    public void setBoolean(PageBuilder pageBuilder, Column c)
    {
        pageBuilder.setNull(c);
    }

    public void setLong(PageBuilder pageBuilder, Column c)
    {
        pageBuilder.setNull(c);
    }

    public void setDouble(PageBuilder pageBuilder, Column c)
    {
        pageBuilder.setNull(c);
    }

    public void setString(PageBuilder pageBuilder, Column c)
    {
        pageBuilder.setNull(c);
    }

    public void setTimestamp(PageBuilder pageBuilder, Column c)
    {
        pageBuilder.setNull(c);
    }

    public void setJson(PageBuilder pageBuilder, Column c)
    {
        pageBuilder.setNull(c);
    }
}
