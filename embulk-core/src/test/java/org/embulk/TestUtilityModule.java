package org.embulk;

//import org.embulk.record.RandomRecordGenerator;
//import org.embulk.record.RandomSchemaGenerator;
import com.google.inject.Binder;
import com.google.inject.Module;

public class TestUtilityModule
        implements Module
{
    @Override
    public void configure(Binder binder) {
        binder.bind(RandomManager.class);
        //binder.bind(RandomRecordGenerator.class);
        //binder.bind(RandomSchemaGenerator.class);
    }
}
