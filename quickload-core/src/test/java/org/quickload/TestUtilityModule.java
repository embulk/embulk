package org.quickload;

import org.quickload.record.RandomManager;
import org.quickload.record.RandomRecordGenerator;
import org.quickload.record.RandomSchemaGenerator;

import com.google.inject.Binder;
import com.google.inject.Module;

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
