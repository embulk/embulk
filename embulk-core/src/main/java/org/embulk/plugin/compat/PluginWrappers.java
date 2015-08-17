package org.embulk.plugin.compat;

import org.embulk.spi.InputPlugin;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.TransactionalFileOutput;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.plugin.compat.InputPluginWrapper;

public class PluginWrappers
{
    public static InputPlugin inputPlugin(InputPlugin input)
    {
        return InputPluginWrapper.wrapIfNecessary(input);
    }

    public static TransactionalFileInput transactionalFileInput(TransactionalFileInput tran)
    {
        return TransactionalFileInputWrapper.wrapIfNecessary(tran);
    }

    public static TransactionalFileOutput transactionalFileOutput(TransactionalFileOutput tran)
    {
        return TransactionalFileOutputWrapper.wrapIfNecessary(tran);
    }

    public static TransactionalPageOutput transactionalPageOutput(TransactionalPageOutput tran)
    {
        return TransactionalPageOutputWrapper.wrapIfNecessary(tran);
    }
}
