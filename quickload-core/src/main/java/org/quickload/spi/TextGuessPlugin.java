package org.quickload.spi;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.quickload.config.NextConfig;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

public abstract class TextGuessPlugin
        implements GuessPlugin
{
    private static interface TextGuessTask
            extends LineDecoderTask
    {
        public void setNewline(Newline newline);
    }

    @Override
    public NextConfig guess(ExecTask exec, ConfigSource config,
            Buffer sample)
    {
        TextGuessTask task;
        try {
            task = exec.loadConfig(config, TextGuessTask.class);
        } catch (Exception ex) {
            return new NextConfig();
        }
        task.setNewline(Newline.CR);

        LineDecoder decoder = new LineDecoder(ImmutableList.of(sample), task);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String line : decoder) {
            if (first) {
                first = false;
            } else {
                sb.append("\r");
            }
            sb.append(sb);
        }
        String text = sb.toString();

        return guessText(exec, config, text);
    }

    public abstract NextConfig guessText(ExecTask exec, ConfigSource config,
            String text);
}
