package org.quickload.standards;

import com.google.inject.Inject;
import org.quickload.exec.BufferManager;
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
    private final BufferManager bufferManager;

    @Inject
    public CsvParserPlugin(BufferManager bufferManager)
    {
        this.bufferManager = bufferManager;
    }

    public BufferOperator openOperator(T task, int processorIndex,
                                       OutputOperator op)
    {
        return new LineCSVOperator(task.getSchema(), processorIndex,
                op, bufferManager);
    }

    public void shutdown()
    {
        // TODO
    }

    // TODO
    /* To consider the design of the framework, CSVParser is implemented with LineOperator.
     * But it should not be used.
     */
    static class LineCSVOperator<T extends ParserTask> extends AbstractLineOperator<T>
    {
        @Inject
        public LineCSVOperator(Schema schema, int processorIndex,
                               OutputOperator op, BufferManager bufferManager)
        {
            super(schema, processorIndex, op, bufferManager);
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