package org.embulk.exec;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.CommitReport;
import org.embulk.type.Schema;
import org.embulk.spi.Exec;
import org.embulk.spi.Page;
import org.embulk.spi.Buffer;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageOutput;
import static org.embulk.spi.Inputs.each;

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

    @Override
    public void transaction(ConfigSource config, ParserPlugin.Control control)
    {
        control.run(Exec.newTaskSource(), null);
    }

    @Override
    public void run(TaskSource taskSource, Schema schema,
            FileInput input, PageOutput output)
    {
        Buffer buffer = getSample(fileBufferInput, maxSampleSize);
        throw new SampledNoticeError(buffer);
    }

    static Buffer runFileInputSampling(ConfigSource config)
    {
        // override in.parser.type so that FileInputPlugin creates GuessParserPlugin
        ConfigSource samplingInputConfig = config.getNested("in").deepCopy();
        samplingInputConfig.getNestedOrSetEmpty("parser").setString("type", "system_sampling");

        final InputPlugin input = Exec.newPlugin(InputPlugin.class, samplingInputConfig.get("type"));
        try {
            input.runInputTransaction(samplingInputConfig, new FileInputPlugin.Control() {
                public List<CommitReport> run(TaskSource inputTaskSource)
                {
                    input.runInput(inputTaskSource, 0, new PageOutput(null) {
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

    private static Buffer getSample(FileInput fileInput, int maxSampleSize)
    {
        Buffer sample = Buffer.allocate(maxSampleSize);
        int sampleSize = 0;

        while (fileInput.nextFile()) {
            for (Buffer buffer : each(fileInput)) {
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
