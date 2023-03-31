/*
 * Copyright 2023 The Embulk project
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

package org.embulk.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the selfupdate subcommand of Embulk.
 *
 * <p>It uses {@link java.net.HttpURLConnection} so that CLI classes do not need additional dependedcies.
 *
 * <p>TODO: Support HTTP(S) proxy. The original Ruby version did not support as well, though.
 */
class License {
    static int printLicenseNotice(final PrintStream out) {
        final Enumeration<URL> resources;
        try {
            resources = License.class.getClassLoader().getResources("META-INF/NOTICE");
        } catch (final IOException ex) {
            logger.error("Failed to load the license notice file at: \"/META-INF/NOTICE\"", ex);
            return -1;
        }

        if (!resources.hasMoreElements()) {
            logger.error("The license notice file \"/META-INF/NOTICE\" not found.");
            return -1;
        }

        final URL firstResource = resources.nextElement();

        if (resources.hasMoreElements()) {
            logger.error("Multiple license notice files \"/META-INF/NOTICE\" found.");
            return -1;
        }

        final Object content;
        try {
            content = firstResource.getContent();
        } catch (final IOException ex) {
            logger.error("I/O error in reading the license notice file \"/META-INF/NOTICE\".", ex);
            return -1;
        }

        if (!(content instanceof InputStream)) {
            logger.error("The license notice file \"/META-INF/NOTICE\" is invalid.");
            return -1;
        }

        try (final InputStream in = (InputStream) content) {
            final byte[] buffer = new byte[4096];

            int length;
            while (-1 != (length = in.read(buffer))) {
                out.write(buffer, 0, length);
            }
        } catch (final IOException ex) {
            logger.error("I/O error in reading/printing the license notice file \"/META-INF/NOTICE\".", ex);
            return -1;
        }

        return 0;
    }

    private static final Logger logger = LoggerFactory.getLogger(License.class);
}
