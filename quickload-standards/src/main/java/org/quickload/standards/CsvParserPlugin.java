package org.quickload.standards;

import org.quickload.record.Column;
import org.quickload.record.DoubleType;
import org.quickload.record.LongType;
import org.quickload.record.RecordProducer;
import org.quickload.record.Schema;
import org.quickload.record.StringType;
import org.quickload.spi.*;

public class CsvParserPlugin<T extends ParserTask>
        implements ParserPlugin<T>
{
    public BufferOperator openOperator(T task, int processorIndex, OutputOperator op)
    {
        return new LineCSVOperator(task.getSchema(), processorIndex, op);
    }

    public void shutdown()
    {
        // TODO
    }

    // TODO extends LineOperator
    static class LineCSVOperator<T extends ParserTask> extends AbstractLineOperator<T>
    {
        public LineCSVOperator(Schema schema, int processorIndex, OutputOperator op)
        {
            super(schema, processorIndex, op);
        }

        @Override
        public void addLine(String line)
        {
            final String[] lineValues = line.split(","); // TODO ad-hoc parsing
            RecordProducer recordProducer = new RecordProducer() {
                @Override
                public void setLong(Column column, LongType.Setter setter) {
                    // TODO setter.setLong(Long.parseLong(lineValues[column.getIndex()]));
                }

                @Override
                public void setDouble(Column column, DoubleType.Setter setter) {
                    // TODO setter.setDouble(Double.parseDouble(lineValues[column.getIndex()]));
                }

                @Override
                public void setString(Column column, StringType.Setter setter) {
                    setter.setString(lineValues[column.getIndex()]);
                }
            };
            schema.produce(recordBuilder, recordProducer);
            recordBuilder.addRecord();
        }
    }
}