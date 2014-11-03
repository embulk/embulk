package org.quickload.record;

import java.util.Arrays;
import org.junit.Ignore;
import com.google.common.base.Joiner;

@Ignore
public class Record
{
    protected Object[] values;

    public Record(Object[] values)
    {
        this.values = values;
    }

    public void prettyPrint()
    {
        // TODO
        for (int i = 0; i < values.length; i++)
        {
            System.out.print(values[i] + " ");
        }
        System.out.println();
    }

    public int size()
    {
        return values.length;
    }

    public Object getObject(int index)
    {
        return values[index];
    }

    public boolean equals(Object obj)
    {
        if (! (obj instanceof Record)) {
            return false;
        }
        Record record = (Record) obj;
        return Arrays.equals(values, record.values);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Record{");
        Joiner.on(", ").appendTo(sb, values);
        sb.append("}");
        return sb.toString();
    }
}
