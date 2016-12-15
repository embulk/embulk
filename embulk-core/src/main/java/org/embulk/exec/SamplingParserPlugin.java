package org.embulk.exec;

import java.util.List;
import com.google.inject.Inject;
import com.google.common.base.Preconditions;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.Schema;
import org.embulk.spi.Exec;
import org.embulk.spi.Page;
import org.embulk.spi.Buffer;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.FileInput;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.PageOutput;
import static org.embulk.spi.util.Inputs.each;

/*
 * Used by FileInputRunner.guess
 */
public class SamplingParserPlugin
        implements ParserPlugin
{
    public static Buffer runFileInputSampling(final FileInputRunner runner, ConfigSource inputConfig)
    {
        // override in.parser.type so that FileInputRunner creates SamplingParserPlugin
        ConfigSource samplingInputConfig = inputConfig.deepCopy();
        samplingInputConfig.getNestedOrSetEmpty("parser").set("type", "system_sampling");
        samplingInputConfig.set("decoders", null);

        try {
            runner.transaction(samplingInputConfig, new InputPlugin.Control() {
                public List<TaskReport> run(TaskSource taskSource, Schema schema, int taskCount)
                {
                    if (taskCount == 0) {
                        throw new NoSampleException("No input files to read sample data");
                    }
                    int maxSize = -1;
                    int maxSizeTaskIndex = -1;
                    for (int taskIndex=0; taskIndex < taskCount; taskIndex++) {
                        try {
                            runner.run(taskSource, schema, taskIndex, new PageOutput() {
                                @Override
                                public void add(Page page)
                                {
                                    throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                                }

                                public void finish() { }

                                public void close() { }
                            });
                        } catch (NotEnoughSampleError ex) {
                            if (maxSize < ex.getSize()) {
                                maxSize = ex.getSize();
                                maxSizeTaskIndex = taskIndex;
                            }
                            continue;
                        }
                    }
                    if (maxSize <= 0) {
                        throw new NoSampleException("All input files are empty");
                    }
                    taskSource.getNested("ParserTaskSource").set("force", true);
                    try {
                        runner.run(taskSource, schema, maxSizeTaskIndex, new PageOutput() {
                            @Override
                            public void add(Page page)
                            {
                                throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                            }

                            public void finish() { }

                            public void close() { }
                        });
                    } catch (NotEnoughSampleError ex) {
                        throw new NoSampleException("All input files are smaller than minimum sampling size");
                    }
                    throw new NoSampleException("All input files are smaller than minimum sampling size");
                }
            });
            throw new AssertionError("SamplingParserPlugin must throw SampledNoticeError");
        } catch (SampledNoticeError error) {
            return error.getSample();
        }
    }

    public static class SampledNoticeError
            extends Error
    {
        private final Buffer sample;

        public SampledNoticeError(Buffer sample)
        {
            this.sample = sample;
        }

        public Buffer getSample()
        {
            return sample;
        }
    }

    public static class NotEnoughSampleError
            extends Error
    {
        private final int size;

        public NotEnoughSampleError(int size)
        {
            this.size = size;
        }

        public int getSize()
        {
            return size;
        }
    }

    private final int minSampleSize;
    private final int sampleSize;

    @Inject
    public SamplingParserPlugin(@ForSystemConfig ConfigSource systemConfig)
    {
        this.minSampleSize = 40;  // empty gzip file is 33 bytes. // TODO get sample size from system config
        this.sampleSize = 32*1024;  // TODO get sample size from system config. See also GuessExecutor.run.
        Preconditions.checkArgument(minSampleSize < sampleSize, "minSampleSize must be smaller than sampleSize");
    }

    @Override
    public void transaction(ConfigSource config, ParserPlugin.Control control)
    {
        control.run(Exec.newTaskSource(), null);
    }

    @Override
    public void run(TaskSource taskSource, Schema schema,
            FileInput input, PageOutput output)
    {
        Buffer buffer = readSample(input, sampleSize);
        if (!taskSource.get(boolean.class, "force", false)) {
            if (buffer.limit() < minSampleSize) {
                throw new NotEnoughSampleError(buffer.limit());
            }
        }
        throw new SampledNoticeError(buffer);
    }

    public static Buffer readSample(FileInput fileInput, int sampleSize)
    {
        return readSample(fileInput, Buffer.allocate(sampleSize), 0, sampleSize);
    }

    public static Buffer readSample(FileInput fileInput, Buffer sample, int offset, int sampleSize)
    {
        if (!fileInput.nextFile()) {
            // no input files
            return sample;
        }

        try {
            for (Buffer buffer : each(fileInput)) {
                int size = Math.min(buffer.limit(), sample.capacity() - offset);
                sample.setBytes(offset, buffer, 0, size);
                offset += size;
                buffer.release();
                if (offset >= sampleSize) {
                    break;
                }
            }
        }
        finally {
            sample.limit(offset);
        }
        return sample;
    }
}
