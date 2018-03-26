package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface ReporterPlugin {
    TaskSource configureTaskSource(final ConfigSource config);

    AbstractReporterImpl open(final TaskSource task);

    /*
     * Reporter API
     *  + report only
     *  BulkLoader creates instances. and they are stored on ExecSession
     *
     * ReporterPlugin
     * (
     * - Reporter (report)
     *   repoter impl (has banckend)
     *     + report( ... structured log ... )
     *     + close, finish. those methods should be used by bulk loader
     * )
     * - Reporter backend + buffer (connector) open, close, write, flush
     *   + open
     *   + flush
     *   + write
     *   + close // (embulk transaction,) the method is called when a task finishes
     *   + cleanup // when embulk transaction is closed, the method is called. (session cleanup)
     *
     *  - create Reporter
     *  - Reporter has repoter backend
     */
}
