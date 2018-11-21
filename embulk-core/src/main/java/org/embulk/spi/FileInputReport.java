package org.embulk.spi;

import java.util.HashMap;
import java.util.Map;

public class FileInputReport {
    private Map<Integer, String> fileNames = new HashMap<>();
    private Map<Integer, Long> expectedSizes = new HashMap<>();

    public FileInputReport() {
    }

    public void setFileName(int taskIndex, String fileName) {
        this.fileNames.put(taskIndex, fileName);
    }

    public void setExpectedSize(int taskIndex, long expectedSize) {
        this.expectedSizes.put(taskIndex, expectedSize);
    }

    public String getFileName(int taskIndex) {
        return this.fileNames.getOrDefault(taskIndex, null);
    }

    public long getExpectedSize(int taskIndex) {
        return this.expectedSizes.getOrDefault(taskIndex, new Integer(0).longValue());
    }
}
