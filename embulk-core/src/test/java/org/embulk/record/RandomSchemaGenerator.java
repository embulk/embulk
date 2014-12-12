package org.embulk.record;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class RandomSchemaGenerator
{
    private final RandomManager randomManager;

    @Inject
    public RandomSchemaGenerator(RandomManager randomManager)
    {
        this.randomManager = randomManager;
    }

    public Schema generate(final int size)
    {
        ImmutableList.Builder<Column> columns = ImmutableList.builder();
        for (int i = 0; i < size; i++) {
            columns.add(generateColumn(i));
        }

        return new Schema(columns.build());
    }

    private Column generateColumn(int index)
    {
        return new Column(index, "c" + index, generateType());
    }

    private Type generateType()
    {
        int index = randomManager.getRandom().nextInt(3);
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
