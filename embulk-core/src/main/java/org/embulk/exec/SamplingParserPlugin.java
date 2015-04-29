package org.embulk.exec;

import java.util.List;
import com.google.inject.Inject;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.CommitReport;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Schema;
import org.embulk.spi.Exec;
import org.embulk.spi.Page;
import org.embulk.spi.Buffer;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.FileInput;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.PageOutput;
import org.embulk.exec.ForSystemConfig;
import static org.embulk.spi.util.Inputs.each;

/*
 * Used by FileInputRunner.guess
 */
public class SamplingParserPlugin
        implements ParserPlugin
{
    private final int maxSampleSize;

    @Inject
    public SamplingParserPlugin(@ForSystemConfig ConfigSource systemConfig)
    {
        this.maxSampleSize = 32*1024;  // TODO get sample syze from system config
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
        Buffer buffer = getSample(input, maxSampleSize);
        throw new SampledNoticeError(buffer);
    }

    public static Buffer runFileInputSampling(final FileInputRunner runner, ConfigSource inputConfig)
    {
        // override in.parser.type so that FileInputRunner creates GuessParserPlugin
        ConfigSource samplingInputConfig = inputConfig.deepCopy();
        samplingInputConfig.getNestedOrSetEmpty("parser").set("type", "system_sampling");
        samplingInputConfig.set("decoders", null);

        try {
            runner.transaction(samplingInputConfig, new InputPlugin.Control() {
                public List<CommitReport> run(TaskSource taskSource, Schema schema, int taskCount)
                {
                    if (taskCount == 0) {
                        throw new NoSampleException("No input files to read sample data");
                    }
                    // TODO repeat runwith taskIndex++ if NoSampleException happens
                    runner.run(taskSource, schema, 0, new PageOutput() {
                        @Override
                        public void add(Page page)
                        {
                            throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                        }

                        public void finish() { }

                        public void close() { }
                    });
                    throw new NoSampleException("No input files to guess parser configuration");
                }
            });
            throw new AssertionError("SamplingParserPlugin must throw SampledNoticeError");
        } catch (SampledNoticeError error) {
            return error.getSample();
        }
    }

    private static Buffer getSample(FileInput fileInput, int maxSampleSize)
    {
        if (!fileInput.nextFile()) {
            // no input files
            return Buffer.EMPTY;
        }

        Buffer sample = Buffer.allocate(maxSampleSize);
        int sampleSize = 0;

        for (Buffer buffer : each(fileInput)) {
            int size = Math.min(buffer.limit(), sample.capacity() - sampleSize);
            sample.setBytes(sampleSize, buffer, 0, size);
            sampleSize += size;
            buffer.release();
            if (sampleSize >= maxSampleSize) {
                break;
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
