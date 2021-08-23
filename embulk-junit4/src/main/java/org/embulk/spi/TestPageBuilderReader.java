package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

public class TestPageBuilderReader {
    public static class MockPageOutput implements PageOutput {
        public List<Page> pages;

        public MockPageOutput() {
            this.pages = new ArrayList<>();
        }

        @Override
        public void add(Page page) {
            pages.add(page);
        }

        @Override
        public void finish() {}

        @Override
        public void close() {}
    }
}
