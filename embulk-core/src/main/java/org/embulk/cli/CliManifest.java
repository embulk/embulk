package org.embulk.cli;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CliManifest {
    private CliManifest() {
        // No instantiation.
    }

    static Manifest getManifest() {
        return Holder.INSTANCE;
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        static final Manifest INSTANCE = getManifest();

        private static Manifest getManifest() {
            try {
                final ProtectionDomain protectionDomain;
                try {
                    protectionDomain = CliManifest.class.getProtectionDomain();
                } catch (final SecurityException ex) {
                    logger.error("Manifest unavailable: ProtectionDomain inaccessible.", ex);
                    return null;
                }

                final CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource == null) {
                    logger.error("Manifest unavailable: CodeSource inaccessible.");
                    return null;
                }

                final URL selfJarUrl = codeSource.getLocation();
                if (selfJarUrl == null) {
                    logger.error("Manifest unavailable: location unavailable from CodeSource.");
                    return null;
                } else if (!selfJarUrl.getProtocol().equals("file")) {
                    logger.error("Manifest unavailable: invalid location: " + selfJarUrl.toString());
                    return null;
                }

                final String selfJarPathString = selfJarUrl.getPath();
                if (selfJarPathString == null) {
                    logger.error("Manifest unavailable: path unavailable in CodeSource location: " + selfJarUrl.toString());
                    return null;
                } else if (selfJarPathString.isEmpty()) {
                    logger.error("Manifest unavailable: empty path from CodeSource location: " + selfJarUrl.toString());
                    return null;
                }

                try (final JarFile selfJarFile = new JarFile(selfJarPathString)) {
                    try {
                        return selfJarFile.getManifest();
                    } catch (final IllegalStateException ex) {
                        logger.error("Manifest unavailable: JAR closed unexpectedly.", ex);
                        return null;
                    } catch (final IOException ex) {
                        logger.error("Manifest unavailable: I/O error in reading manifest.", ex);
                        return null;
                    }
                } catch (final SecurityException ex) {
                    logger.error("Manifest unavailable: JAR file inaccessible.", ex);
                    return null;
                } catch (final IOException ex) {
                    logger.error("Manifest unavailable: I/O error in reading JAR.", ex);
                    return null;
                }
            } catch (final Throwable ex) {
                logger.error("Manifest unavailable: unknown error.", ex);
                return null;
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CliManifest.class);
}
