package org.embulk.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.embulk.deps.maven.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the selfupdate subcommand of Embulk.
 *
 * <p>It uses {@link java.net.HttpURLConnection} so that CLI classes do not need additional dependedcies.
 *
 * <p>TODO: Support HTTP(S) proxy. The original Ruby version did not support as well, though.
 */
class SelfUpdate {
    static void toLatest(final String runningVersion, final boolean forced) throws IOException {
        final Path pathRunning = identifyRunningJarPath();
        final String userAgent = "Embulk/" + runningVersion;

        logger.info("Looking for the latest version");
        final String targetVersion = findLatestVersion(userAgent);
        if (ComparableVersion.of(targetVersion).compareTo(ComparableVersion.of(runningVersion)) <= 0) {
            logger.info("Already up-to-date. The latest version is: {}", runningVersion);
            return;
        }

        update(targetVersion, forced, pathRunning, userAgent);
    }

    static void toSpecific(final String runningVersion, final String targetVersion, final boolean forced) throws IOException {
        final Path pathRunning = identifyRunningJarPath();
        update(targetVersion, forced, pathRunning, "Embulk/" + runningVersion);
    }

    private static void update(
            final String targetVersion,
            final boolean forced,
            final Path pathRunning,
            final String userAgent) throws IOException {
        logger.info("Updating to the version {}", targetVersion);

        final String targetUrlString = String.format("https://dl.embulk.org/embulk-%s.jar", targetVersion);

        // In case of Windows, just download without overwriting the running JAR file.
        final String osName = System.getProperty("os.name");
        if (osName != null && osName.contains("Windows")) {
            final Path pathDownloaded = Files.createTempFile("embulk-" + targetVersion + "-", ".jar");
            logger.warn("Overwriting a running JAR file is not permitted on Windows. Just downloading to: {}", pathDownloaded);
            download(targetUrlString, pathDownloaded, userAgent);
            return;
        }

        final Path pathTemporary = Files.createTempFile("embulk-selfupdate", ".jar");
        try {
            download(targetUrlString, pathTemporary, userAgent);

            final FileSystem fileSystemOriginal = pathRunning.getFileSystem();
            final FileSystem fileSystemTemporary = pathTemporary.getFileSystem();
            if (fileSystemOriginal.supportedFileAttributeViews().contains("posix")
                        && fileSystemTemporary.supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(pathTemporary, Files.getPosixFilePermissions(pathRunning));
            }

            if (!forced) {  // Check corruption
                final String downloadedVersion = extractImplementationVersionFromJar(pathTemporary);
                if (!downloadedVersion.equals(targetVersion)) {
                    throw new IOException(String.format("Downloaded version does not match: %s (downloaded) / %s (target)",
                                                        downloadedVersion, targetVersion));
                }
            }
            logger.info("Overwriting {} onto {}", pathTemporary, pathRunning);
            Files.move(pathTemporary, pathRunning, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Overwritten");
        } finally {
            Files.deleteIfExists(pathTemporary);
        }
        logger.info("Updated to the version {}", targetVersion);
    }

    private static void download(final String targetUrlString, final Path path, final String userAgent) throws IOException {
        logger.info("Started download from: " + targetUrlString);

        String urlString = targetUrlString;
        for (int i = 0; i < MAXIMUM_REDIRECTS; ++i) {
            final Optional<String> nextUrlString = getDownloadedOrNextUrl(urlString, path, userAgent);
            if (!nextUrlString.isPresent()) {
                return;
            }
            logger.info("Redirected to: " + nextUrlString.get());
            urlString = nextUrlString.get();
        }
        throw new IOException("Too many redirects from: " + targetUrlString);
    }

    private static Optional<String> getDownloadedOrNextUrl(final String urlString, final Path path, final String userAgent)
            throws IOException {
        logger.debug("Requesting GET {}", urlString);
        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException ex) {
            throw new IOException("Invalid URL: " + urlString, ex);
        }

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            // Need to follow redirects for GET, not HEAD, here because:
            // * https://dl.bintray.com/... returns 200 for HEAD, and 302 for GET
            // * https://github-production-release-asset-XXXXXX.s3.amazonaws.com/... returns 200 for GET, and 403 for HEAD
            connection.setInstanceFollowRedirects(false);

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "*/*");

            // Cloudflare requires User-Agent.
            connection.setRequestProperty("User-Agent", userAgent);

            connection.connect();

            final int statusCode = connection.getResponseCode();
            logger.debug("Received HTTP status code {} from GET {}", statusCode, urlString);
            switch (statusCode) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    final String location = connection.getHeaderField("Location");
                    if (location == null) {
                        throw new IOException("No Location header for HTTP status code " + statusCode);
                    }
                    return Optional.of(location);
                case HttpURLConnection.HTTP_OK:
                    final InputStream downloadStream = connection.getInputStream();
                    logger.info("Downloading from {} into {}", urlString, path.toString());
                    // It should be okay to replace the temporary file created by |Files.createTempFile|.
                    Files.copy(downloadStream, path, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Downloaded");
                    return Optional.empty();
                default:
                    final String responseMessage;
                    try {
                        responseMessage = connection.getResponseMessage();
                    } catch (final IOException ex) {
                        throw new IOException("Unexpected HTTP status code " + statusCode, ex);
                    }
                    throw new IOException("Unexpected HTTP status code " + statusCode + " with message: " + responseMessage);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String extractImplementationVersionFromJar(final Path jarPath) throws IOException {
        final String implementationVersion;
        try (final JarFile jarFile = new JarFile(jarPath.toFile())) {
            final Manifest manifest = jarFile.getManifest();

            final String manifestContents;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                manifest.write(outputStream);
                manifestContents = outputStream.toString();
            }

            final Attributes mainAttributes = manifest.getMainAttributes();
            implementationVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (final IOException ex) {
            throw new IOException("Downloaded an invalid JAR file: No Implementation-Version in MANIFEST", ex);
        }

        if (implementationVersion == null) {
            throw new IOException("Downloaded an invalid JAR file: No Implementation-Version in MANIFEST");
        }
        return implementationVersion;

        // NOTE: Checking embulk/version.rb is no longer needed.
        // "Implementation-Version" has been included in MANIFEST of all-in-one Embulk JARs since v0.4.0.
    }

    private static Path identifyRunningJarPath() throws IOException {
        final URI uri;
        try {
            uri = SelfUpdate.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (final URISyntaxException ex) {
            throw new IOException("URI of the Embulk installation path is invalid", ex);
        }
        final Path path = Paths.get(uri);

        // The check was against Gem-based Embulk. but it is not effective now as Gem versions are no longer available.
        // TODO: Check installation with multiple JAR files with dependencies.
        if ((!Files.exists(path)) || (!Files.isRegularFile(path))) {
            throw new IOException("This Embulk installation is not an all-in-one JAR. \"selfupdate\" does not work");
        }
        return path;
    }

    private static String findLatestVersion(final String userAgent) throws IOException {
        final String initialUrlString = "https://dl.embulk.org/embulk-latest.jar";
        logger.info("Started lookup from: " + initialUrlString);

        String urlString = initialUrlString;
        for (int i = 0; i < MAXIMUM_REDIRECTS; ++i) {
            final Optional<String> nextUrlString = headNextUrl(urlString, userAgent);
            if (!nextUrlString.isPresent()) {
                throw new IOException("Reached at non-redirecting URL unexpectedly: " + urlString);
            }
            logger.info("Redirected to: " + nextUrlString.get());
            final Optional<String> guessedVersion = guessVersionFromUrl(nextUrlString.get());
            if (guessedVersion.isPresent()) {
                logger.info("Supposed to be version: " + guessedVersion.get());
                return guessedVersion.get();
            }
            urlString = nextUrlString.get();
        }
        throw new IOException("Too many redirects from: " + initialUrlString);
    }

    private static Optional<String> headNextUrl(final String urlString, final String userAgent) throws IOException {
        logger.debug("Requesting HEAD {}", urlString);
        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException ex) {
            throw new IOException("Invalid URL: " + urlString, ex);
        }

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("Accept", "*/*");

            // Cloudflare requires User-Agent.
            connection.setRequestProperty("User-Agent", userAgent);

            connection.connect();

            final int statusCode = connection.getResponseCode();
            logger.debug("Received HTTP status code {} from HEAD {}", statusCode, urlString);
            switch (statusCode) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    final String location = connection.getHeaderField("Location");
                    if (location == null) {
                        throw new IOException("No Location header for HTTP status code " + statusCode);
                    }
                    return Optional.of(location);
                case HttpURLConnection.HTTP_OK:
                    return Optional.empty();
                default:
                    final String responseMessage;
                    try {
                        responseMessage = connection.getResponseMessage();
                    } catch (final IOException ex) {
                        throw new IOException("Unexpected HTTP status code " + statusCode, ex);
                    }
                    throw new IOException("Unexpected HTTP status code " + statusCode + " with message: " + responseMessage);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static Optional<String> guessVersionFromUrl(final String urlString) {
        final Matcher matcherFile = FILE_URL_PATTERN.matcher(urlString);
        if (matcherFile.matches()) {
            return Optional.of(matcherFile.group(1));
        }

        final Matcher matcherDir = DIR_URL_PATTERN.matcher(urlString);
        if (matcherDir.matches()) {
            return Optional.of(matcherDir.group(1));
        }

        return Optional.empty();
    }

    static String guessVersionFromUrlForTesting(final String urlString) {
        return guessVersionFromUrl(urlString).orElse(null);
    }

    private static final Logger logger = LoggerFactory.getLogger(SelfUpdate.class);

    // https://bintray.com/embulk/maven/embulk/0.8.27
    private static final Pattern DIR_URL_PATTERN = Pattern.compile("^https?://.*/embulk/(\\d+\\.\\d+[^\\/]+).*$");

    // https://dl.bintray.com/embulk/maven/embulk-0.9.6.jar
    // https://dl.embulk.org/embulk-0.9.15.jar
    // https://github.com/embulk/embulk/releases/download/v0.9.16/embulk-0.9.16.jar
    private static final Pattern FILE_URL_PATTERN = Pattern.compile("^https://.*/embulk-(\\d+\\.\\d+[^\\/]+)\\.jar$");

    private static final int MAXIMUM_REDIRECTS = 8;
}
