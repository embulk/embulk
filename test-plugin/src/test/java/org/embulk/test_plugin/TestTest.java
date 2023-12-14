package org.embulk.test_plugin;

import org.embulk.junit5.EmbulkPluginTest;

public class TestTest {
    @EmbulkPluginTest
    public void testSuccess() {
        System.out.println("running testSuccess");
    }

    @EmbulkPluginTest
    public void testThrow() {
        throw new RuntimeException("running testThrow!!");
    }

    public void noTest() {
        System.out.println("running noTest");
        throw new RuntimeException("!!!!");
    }
}
