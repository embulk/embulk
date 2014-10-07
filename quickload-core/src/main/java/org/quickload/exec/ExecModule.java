package org.quickload.exec;

import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import org.quickload.config.ModelManager;
import org.quickload.record.TypeManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class ExecModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        checkNotNull(binder, "binder is null.");
        binder.bind(ModelManager.class).in(Scopes.SINGLETON);
        binder.bind(TypeManager.class).in(Scopes.SINGLETON);
    }
}
