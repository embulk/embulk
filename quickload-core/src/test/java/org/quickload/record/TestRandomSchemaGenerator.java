package org.quickload.record;


import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;

@Ignore
public class TestRandomSchemaGenerator extends RandomTestUtils {

    public Schema generate(final int size) throws Exception
    {
        List<Column> columns = new ArrayList<Column>(size);
        for (int i = 0; i < size; i++) {
            columns.add(generateColumn(i));
        }

        Schema schema = new Schema(columns);
        System.out.println(schema.toString());
        return new Schema(columns);
    }

    private Column generateColumn(int index) throws Exception
    {
        return new Column(index, "c" + index, generateType());
    }

    private Type generateType() throws Exception
    {
        int index = random.nextInt(3);
        if (index == 0) {
            return LongType.LONG;
        } else if (index == 1) {
            return DoubleType.DOUBLE;
        } else if (index == 2) {
            return StringType.STRING;
        } else {
            throw new AssertionError("IndexOutOfBounds: " + index);
        }
    }
}
