package org.quickload.exec;

import java.util.List;
import org.quickload.buffer.Buffer;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.record.Page;
import org.quickload.spi.ExecTask;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.ExecControl;

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
                    sample.setBytes(sampleSize, buffer, 0, buffer.limit());
                    sampleSize += buffer.limit();
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
