package org.embulk.cli;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;

// It uses |java.net.HttpURLConnection| so that the CLI classes do not need additional dependedcies.
// TODO(dmikurube): Support HTTP proxy. The original Ruby version did not support as well, though.
public class EmbulkSelfUpdate {
    // TODO(dmikurube): Stop catching Exceptions here when embulk_run.rb is replaced to Java.
    public void updateSelf(final String runningVersionString,
                           final String specifiedVersionString,
                           final boolean isForced) throws IOException, URISyntaxException {
        try {
            updateSelfWithExceptions(runningVersionString, specifiedVersionString, isForced);
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void updateSelfWithExceptions(final String runningVersionString,
                                          final String specifiedVersionString,
                                          final boolean isForced) throws IOException, URISyntaxException {
        final Path jarPathJava = Paths.get(
                EmbulkSelfUpdate.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

        if ((!Files.exists(jarPathJava)) || (!Files.isRegularFile(jarPathJava))) {
            throw exceptionNoSingleJar();
        }

        final String targetVersionString;
        if (specifiedVersionString != null) {
            System.out.printf("Checking version %s...\n", specifiedVersionString);
            targetVersionString = checkTargetVersion(specifiedVersionString);
            if (targetVersionString == null) {
                throw new RuntimeException(String.format("Specified version does not exist: %s", specifiedVersionString));
            }
            System.out.printf("Found version %s.\n", specifiedVersionString);
        } else {
            System.out.println("Checking the latest version...");
            final ComparableVersion runningVersion = new ComparableVersion(runningVersionString);
            targetVersionString = checkLatestVersion();
            final ComparableVersion targetVersion = new ComparableVersion(targetVersionString);
            if (targetVersion.compareTo(runningVersion) <= 0) {
                System.out.printf("Already up-to-date. %s is the latest version.\n", runningVersion);
                return;
            }
            System.out.printf("Found a newer version %s.\n", targetVersion);
        }

        if (!Files.isWritable(jarPathJava)) {
            throw new RuntimeException(String.format("The existing %s is not writable. May need to sudo?",
                                                     jarPathJava.toString()));
        }

        final URL downloadUrl = new URL(String.format("https://dl.bintray.com/embulk/maven/embulk-%s.jar",
                                                      targetVersionString));
        System.out.printf("Downloading %s ...\n", downloadUrl.toString());

        Path jarPathTemp = Files.createTempFile("embulk-selfupdate", ".jar");
        try {
            final HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
            try {
                // Follow the redicrect from the Bintray URL.
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                connection.connect();
                final int statusCode = connection.getResponseCode();
                if (HttpURLConnection.HTTP_OK != statusCode) {
                    throw new FileNotFoundException(
                            String.format("Unexpected HTTP status code: %d", statusCode));
                }
                InputStream input = connection.getInputStream();
                // TODO(dmikurube): Confirm if it is okay to replace a temp file created by Files.createTempFile.
                Files.copy(input, jarPathTemp, StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(jarPathTemp, Files.getPosixFilePermissions(jarPathJava));
            } finally {
                connection.disconnect();
            }

            if (!isForced) {  // Check corruption
                final String versionJarTemp;
                try {
                    versionJarTemp = getJarVersion(jarPathTemp);
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(
                            "Failed to check corruption. Downloaded version may include incompatible changes. Try the '-f' option to force updating without checking.",
                            ex);
                }
                if (!versionJarTemp.equals(targetVersionString)) {
                    throw new RuntimeException(
                            String.format("Downloaded version does not match: %s (downloaded) / %s (target)",
                                          versionJarTemp,
                                          targetVersionString));
                }
            }
            Files.move(jarPathTemp, jarPathJava, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(jarPathTemp);
        }
        System.out.println(String.format("Updated to %s.", targetVersionString));
    }

    private RuntimeException exceptionNoSingleJar() {
        return new RuntimeException(
                "Embulk is not installed as a single jar. \"selfupdate\" does not work. If you installed Embulk through gem, run \"gem install embulk\" instead.");
    }

    /**
     * Checks the latest version from bintray.com.
     *
     * It passes all {@code IOException} and {@code RuntimeException} through out.
     */
    private String checkLatestVersion() throws IOException {
        final URL bintrayUrl = new URL("https://bintray.com/embulk/maven/embulk/_latestVersion");
        final HttpURLConnection connection = (HttpURLConnection) bintrayUrl.openConnection();
        try {
            // Stop HttpURLConnection from following redirects when the status code is 301 or 302.
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.connect();
            final int statusCode = connection.getResponseCode();
            if (HttpURLConnection.HTTP_MOVED_TEMP != statusCode) {
                throw new FileNotFoundException(
                        String.format("Unexpected HTTP status code: %d", statusCode));
            }
            final String location = connection.getHeaderField("Location");
            final Matcher versionMatcher = VERSION_URL_PATTERN.matcher(location);
            if (!versionMatcher.matches()) {
                throw new FileNotFoundException(
                        String.format("Invalid version number in \"Location\" header: %s", location));
            }
            return versionMatcher.group(1);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Checks the target version in bintray.com.
     *
     * It passes all {@code IOException} and {@code RuntimeException} through out.
     */
    private String checkTargetVersion(String version) throws IOException {
        final URL bintrayUrl = new URL(String.format("https://bintray.com/embulk/maven/embulk/%s", version));
        final HttpURLConnection connection = (HttpURLConnection) bintrayUrl.openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.connect();
            final int statusCode = connection.getResponseCode();
            if (HttpURLConnection.HTTP_NOT_FOUND == statusCode) {
                return null;
            } else if (HttpURLConnection.HTTP_OK != statusCode) {
                throw new FileNotFoundException(
                        String.format("Unexpected HTTP status code: %d", statusCode));
            } else {
                return version;
            }
        } finally {
            connection.disconnect();
        }
    }

    private String getJarVersion(Path jarPath) throws IOException {
        try (final JarFile jarFile = new JarFile(jarPath.toFile())) {
            final Manifest manifest;
            try {
                manifest = jarFile.getManifest();
            } catch (IOException ex) {
                throw new IOException("Version not found. Failed to load the manifest.", ex);
            }
            String manifestContents;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                manifest.write(outputStream);
                manifestContents = outputStream.toString();
            } catch (IOException ex) {
                manifestContents = "(Failed to read the contents of the manifest.)";
            }
            final Attributes mainAttributes = manifest.getMainAttributes();
            final String implementationVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (implementationVersion == null) {
                throw new IOException("Version not found. Failed to read \""
                                      + Attributes.Name.IMPLEMENTATION_VERSION
                                      + "\": "
                                      + manifestContents);
            }
            return implementationVersion;
        } catch (IOException ex) {
            throw new IOException("Version not found. Failed to load the jar file.", ex);
        }

        // NOTE: Checking embulk/version.rb is no longer needed.
        // The jar manifest with "Implementation-Version" has been included in Embulk jars from v0.4.0.
    }

    private static final Pattern VERSION_URL_PATTERN = Pattern.compile("^https?://.*/embulk/(\\d+\\.\\d+[^\\/]+).*$");
    private static final Pattern VERSION_RUBY_PATTERN = Pattern.compile("^\\s*VERSION\\s*\\=\\s*(\\p{Graph}+)\\s*$");
}
