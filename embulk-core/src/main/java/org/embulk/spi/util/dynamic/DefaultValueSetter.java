package org.embulk.spi.util.dynamic;

import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.time.Timestamp;

public interface DefaultValueSetter
{
    public void setBoolean(PageBuilder pageBuilder, Column c);

    public void setLong(PageBuilder pageBuilder, Column c);

    public void setDouble(PageBuilder pageBuilder, Column c);

    public void setString(PageBuilder pageBuilder, Column c);

    public void setTimestamp(PageBuilder pageBuilder, Column c);
}
