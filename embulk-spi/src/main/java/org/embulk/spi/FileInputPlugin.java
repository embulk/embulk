/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi;

import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;

public interface FileInputPlugin {
    interface Control {
        List<TaskReport> run(TaskSource taskSource,
                int taskCount);
    }

    ConfigDiff transaction(ConfigSource config,
            FileInputPlugin.Control control);

    ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileInputPlugin.Control control);

    void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports);

    TransactionalFileInput open(TaskSource taskSource,
            int taskIndex);
}
