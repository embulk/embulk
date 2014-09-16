package org.quickload.spi;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class DynamicReport
        implements Report
{
    public static class Builder
            implements ReportBuilder
    {
        private ImmutableMap.Builder<String, Object> builder;

        public Builder put(String key, Object value)
        {
            builder.put(key, value);
            return this;
        }

        public Builder put(Map.Entry<? extends String, ? extends Object> entry)
        {
            builder.put(entry);
            return this;
        }

        public Builder putAll(Map<? extends String, ? extends Object> map)
        {
            builder.putAll(map);
            return this;
        }

        public DynamicReport build(Report nextReport)
        {
            builder.put("nextReport", nextReport);
            return new DynamicReport(builder.build());
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private final Map<String, Object> map;

    public DynamicReport(Map<String, Object> map)
    {
        this.map = map;
    }

    public Report getNextReport()
    {
        return (Report) map.get("nextReport");
    }

    // TODO serialize / deserialize
}
