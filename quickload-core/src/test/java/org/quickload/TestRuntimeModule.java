package org.quickload;

import com.google.inject.Module;
import com.google.inject.Binder;
import org.quickload.exec.ExecModule;
import org.quickload.plugin.BuiltinPluginSourceModule;

public class TestRuntimeModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        new ExecModule().configure(binder);
        new BuiltinPluginSourceModule().configure(binder);
    }
}
