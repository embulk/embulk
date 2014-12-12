package org.embulk.record;

import java.util.Iterator;

import com.google.inject.Inject;

public class RandomRecordGenerator
{
    private final RandomManager randomManager;

    @Inject
    public RandomRecordGenerator(RandomManager randomManager)
    {
        this.randomManager = randomManager;
    }

    public Iterable<Record> generate(final Schema schema, final int count)
    {
        return new Iterable<Record>() {
            public Iterator<Record> iterator() {
                return new Iterator<Record>() {
                    private int number;

                    @Override
                    public boolean hasNext()
                    {
                        return count > number;
                    }

                    @Override
                    public Record next()
                    {
                        number += 1;
                        return generateRow(schema);
                    }

                    @Override
                    public void remove()
                    {
                        throw new AssertionError("NotSupported");
                    }
                };
            }
        };
    }

    private Record generateRow(Schema schema)
    {
        Object[] values = new Object[schema.getColumnCount()];
        for (int j = 0; j < values.length; j++) {
            values[j] = generateValue(schema.getColumn(j));
        }
        return new Record(values);
    }

    private Object generateValue(Column column)
    {
        Type type = column.getType();
        if (type.equals(LongType.LONG)) {
            return randomManager.getRandom().nextLong();
        } else if (type.equals(DoubleType.DOUBLE)) {
            return randomManager.getRandom().nextDouble();
        } else if (type.equals(StringType.STRING)) {
            return "muga"; // TODO
        } else {
            throw new AssertionError("NotSupportedType: " + column);
        }
    }
}
