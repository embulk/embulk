package org.quickload.record;

import org.junit.Ignore;
import com.google.common.base.Joiner;

@Ignore
public class Row {

    protected Object[] records;

    public Row(Object[] records)
    {
        this.records = records;
    }

    public void prettyPrint()
    {
        // TODO
        for (int i = 0; i < records.length; i++)
        {
            System.out.print(records[i] + " ");
        }
        System.out.println();
    }

    public int size()
    {
        return records.length;
    }

    public Object getRecord(int index)
    {
        return records[index];
    }

    public boolean equals(Object obj)
    {
        if (! (obj instanceof Row)) {
            return false;
        }
        Row row = (Row) obj;
        return Arrays.equals(records, row.records);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Record{");
        Joiner.on(", ").appendTo(sb, records);
        sb.append("}");
        return sb.toString();
    }
}
