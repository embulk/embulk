package org.quickload.record;

import org.junit.Ignore;

import java.util.List;

import static org.junit.Assert.assertEquals;

@Ignore
public class Assert {

    public static void assertRowsEquals(List<Row> expected, List<Row> actual)
    {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i ++) {
            assertRowEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertRowEquals(Row expected, Row actual)
    {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i ++) {
            assertEquals(expected.getRecord(i), actual.getRecord(i));
        }
    }
}
