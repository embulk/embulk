package org.embulk.exec;

import java.util.List;
import org.embulk.buffer.Buffer;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.Report;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageOutput;
import org.embulk.record.Page;
import org.embulk.spi.ExecTask;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.ExecControl;

/*
 * Used by GuessExecutor
 */
class SamplingParserPlugin
        implements ParserPlugin
{
    private final int maxSampleSize;

    public SamplingParserPlugin(int maxSampleSize)
    {
        this.maxSampleSize = maxSampleSize;
    }

    public TaskSource getParserTask(ExecTask exec, ConfigSource config)
    {
        return new TaskSource();
    }

    public void runParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        throw new SampledNoticeError(getSample(fileBufferInput, maxSampleSize));
    }

    static Buffer runFileInputSampling(final ExecTask exec, ConfigSource config)
    {
        // override in.parser.type so that FileInputPlugin creates GuessParserPlugin
        ConfigSource samplingInputConfig = config.getObject("in").deepCopy();
        samplingInputConfig.getObjectOrSetEmpty("parser").setString("type", "system_sampling");

        final InputPlugin input = exec.newPlugin(InputPlugin.class, samplingInputConfig.get("type"));
        try {
            input.runInputTransaction(exec, samplingInputConfig, new ExecControl() {
                public List<Report> run(TaskSource inputTaskSource)
                {
                    input.runInput(exec, inputTaskSource, 0, new PageOutput(null) {
                        @Override
                        public void add(Page page)
                        {
                            throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                        }
                    });
                    throw new NoSampleException("No input files to guess parser configuration");
                }
            });
            throw new AssertionError("Executor must throw exceptions");
        } catch (SampledNoticeError error) {
            return error.getSample();
        }
    }

    private static Buffer getSample(FileBufferInput fileBufferInput, int maxSampleSize)
    {
        Buffer sample = Buffer.allocate(maxSampleSize);
        int sampleSize = 0;

        while (fileBufferInput.nextFile()) {
            for (Buffer buffer : fileBufferInput) {
                if (sampleSize >= maxSampleSize) {
                    // skip remaining all buffers so that FileInputPlugin.runInput doesn't
                    // throw exceptions at channel.join()
                } else {
                    int size = Math.min(buffer.limit(), sample.capacity() - sampleSize);
                    sample.setBytes(sampleSize, buffer, 0, size);
                    sampleSize += size;
                }
                buffer.release();
            }
        }

        sample.limit(sampleSize);
        return sample;
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
}
