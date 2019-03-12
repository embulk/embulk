package org.embulk.deps.cli;

import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public final class VelocityEngineDelegateImpl extends VelocityEngineDelegate {
    public VelocityEngineDelegateImpl() {
        this.velocityEngine = new VelocityEngine();
        this.velocityEngine.init();
        this.velocityEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
                                        "org.apache.velocity.runtime.log.NullLogSystem");
    }

    @Override
    public boolean evaluate(
            final Map<String, String> contextMap,
            final Writer writer,
            final String logTag,
            final Reader reader) {
        return this.velocityEngine.evaluate(new VelocityContext(contextMap), writer, logTag, reader);
    }

    private final VelocityEngine velocityEngine;
}
