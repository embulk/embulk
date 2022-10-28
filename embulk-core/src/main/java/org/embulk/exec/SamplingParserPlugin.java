package org.embulk.exec;

import static java.util.Locale.ENGLISH;
import static org.embulk.spi.util.Inputs.each;

import com.google.common.base.Preconditions;
import java.text.NumberFormat;
import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferImpl;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Used by FileInputRunner.guess
 */
public class SamplingParserPlugin implements ParserPlugin {
    public static Buffer runFileInputSampling(final FileInputRunner runner, ConfigSource inputConfig) {
        return runFileInputSampling(runner, inputConfig, Exec.newConfigSource());
    }

    public static Buffer runFileInputSampling(final FileInputRunner runner, ConfigSource inputConfig, ConfigSource sampleBufferConfig) {
        final SampleBufferTask sampleBufferTask = loadSampleBufferTask(sampleBufferConfig);

        // override in.parser.type so that FileInputRunner creates SamplingParserPlugin
        ConfigSource samplingInputConfig = inputConfig.deepCopy();
        samplingInputConfig.getNestedOrSetEmpty("parser")
                .set("type", "system_sampling")
                .set("sample_buffer_bytes", sampleBufferTask.getSampleBufferBytes());
        samplingInputConfig.set("decoders", null);

        try {
            runner.transaction(samplingInputConfig, new InputPlugin.Control() {
                    public List<TaskReport> run(TaskSource taskSource, Schema schema, int taskCount) {
                        if (taskCount == 0) {
                            throw new NoSampleException("No input files to read sample data");
                        }
                        int maxSize = -1;
                        int maxSizeTaskIndex = -1;
                        for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
                            try {
                                runner.run(taskSource, schema, taskIndex, new PageOutput() {
                                        @Override
                                        public void add(Page page) {
                                            throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                                        }

                                        public void finish() {}

                                        public void close() {}
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
                                    public void add(Page page) {
                                        throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                                    }

                                    public void finish() {}

                                    public void close() {}
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

    public static class SampledNoticeError extends Error {
        private final Buffer sample;

        public SampledNoticeError(Buffer sample) {
            this.sample = sample;
        }

        public Buffer getSample() {
            return sample;
        }
    }

    public static class NotEnoughSampleError extends Error {
        private final int size;

        public NotEnoughSampleError(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SamplingParserPlugin.class);

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(ENGLISH);
    private final int minSampleBufferBytes;

    public interface PluginTask extends Task, SampleBufferTask {}

    public interface SampleBufferTask extends Task {
        @Config("sample_buffer_bytes")
        @ConfigDefault("32768") // 32 * 1024
        public int getSampleBufferBytes();
    }

    public SamplingParserPlugin() {
        this.minSampleBufferBytes = 40;  // empty gzip file is 33 bytes. // TODO get sample size from system config
    }

    @Override
    public void transaction(ConfigSource config, ParserPlugin.Control control) {
        final PluginTask task = loadPluginTask(config);
        Preconditions.checkArgument(minSampleBufferBytes < task.getSampleBufferBytes(), "minSampleBufferBytes must be smaller than sample_buffer_bytes");

        logger.info("Try to read {} bytes from input source", numberFormat.format(task.getSampleBufferBytes()));
        control.run(task.dump(), null);
    }

    @Override
    public void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput output) {
        final PluginTask task = loadPluginTaskFromTaskSource(taskSource);
        Buffer buffer = readSample(input, task.getSampleBufferBytes());
        if (!taskSource.get(boolean.class, "force", false)) {
            if (buffer.limit() < minSampleBufferBytes) {
                throw new NotEnoughSampleError(buffer.limit());
            }
        }
        throw new SampledNoticeError(buffer);
    }

    @Override
    public TaskReport runWithResult(final TaskSource taskSource, final Schema schema, final FileInput input, final PageOutput output)
    {
        return null;
    }

    public static Buffer readSample(FileInput fileInput, int sampleBufferBytes) {
        return readSample(fileInput, BufferImpl.allocate(sampleBufferBytes), 0, sampleBufferBytes);
    }

    public static Buffer readSample(FileInput fileInput, Buffer sample, int offset, int sampleBufferBytes) {
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
                if (offset >= sampleBufferBytes) {
                    break;
                }
            }
        } finally {
            sample.limit(offset);
        }
        return sample;
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static SampleBufferTask loadSampleBufferTask(final ConfigSource config) {
        return config.loadConfig(SampleBufferTask.class);
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static PluginTask loadPluginTask(final ConfigSource config) {
        return config.loadConfig(PluginTask.class);
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static PluginTask loadPluginTaskFromTaskSource(final TaskSource taskSource) {
        return taskSource.loadTask(PluginTask.class);
    }
}
