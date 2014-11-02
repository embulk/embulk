package org.quickload;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.junit.Ignore;
import org.quickload.exec.BufferManager;
import org.quickload.record.RandomManager;

@Ignore
public class TestExecModule
        implements Module
{
    @Override
    public void configure(Binder binder) {
        binder.bind(BufferManager.class).in(Scopes.SINGLETON);
        binder.bind(RandomManager.class).in(Scopes.SINGLETON);
    }
}
