package org.quickload;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.junit.Ignore;
import org.quickload.exec.BufferManager;
import org.quickload.record.RandomManager;
import org.quickload.record.RandomRecordGenerator;
import org.quickload.record.RandomSchemaGenerator;

@Ignore
public class TestUtilityModule
        implements Module
{
    @Override
    public void configure(Binder binder) {
        binder.bind(RandomManager.class);
        binder.bind(RandomRecordGenerator.class);
        binder.bind(RandomSchemaGenerator.class);
    }
}
