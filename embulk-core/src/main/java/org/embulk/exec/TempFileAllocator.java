package org.embulk.exec;

import java.io.File;
import com.google.inject.Inject;
import org.embulk.config.ConfigSource;
import org.embulk.spi.TempFileSpace;

// TODO change this class to interface
// TODO don't use this class directly. Use spi.Exec.getTempFileSpace() instead.
public class TempFileAllocator
{
    private final File[] dirs;

    @Inject
    public TempFileAllocator(@ForSystemConfig ConfigSource systemConfig)
    {
        // TODO get `temp_dirs` from system config
        String s = System.getProperty("java.io.tmpdir");
        if (s == null || s.isEmpty()) {
            s = "/tmp";
        }
        this.dirs = new File[] {
            new File(s, "embulk")
        };
    }

    public TempFileSpace newSpace(String subdir)
    {
        // TODO support multiple directories
        // UNIX/Linux cannot include '/' as file name.
        // Windows cannot include ':' as file name.
        subdir = subdir.replace('/', '-').replace(':', '-');
        return new TempFileSpace(new File(dirs[0], subdir));
    }
}
