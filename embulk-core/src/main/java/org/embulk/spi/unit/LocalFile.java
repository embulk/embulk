package org.embulk.spi.unit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.embulk.spi.Exec;
import org.embulk.spi.TempFileException;
import org.embulk.spi.TempFileSpace;

public class LocalFile {
    public static LocalFile of(File path) throws IOException {
        return of(path.toPath());
    }

    public static LocalFile of(Path path) throws IOException {
        return new LocalFile(path, Files.readAllBytes(path));
    }

    public static LocalFile of(String path) throws IOException {
        return of(Paths.get(path));
    }

    public static LocalFile ofContent(byte[] content) {
        return new LocalFile(content);
    }

    public static LocalFile ofContent(String content) {
        return new LocalFile(content.getBytes(StandardCharsets.UTF_8));
    }

    private Path path;
    private final byte[] content;

    private LocalFile(Path path, byte[] content) {
        this.path = path;
        this.content = content;
    }

    private LocalFile(byte[] content) {
        this.path = null;
        this.content = content;
    }

    public File getFile() {
        return getPath(Exec.getTempFileSpace()).toFile();
    }

    public File getFile(TempFileSpace space) {
        return getPath(space).toFile();
    }

    public Path getPath() {
        return getPath(Exec.getTempFileSpace());
    }

    public synchronized Path getPath(TempFileSpace tempFileSpace) {
        if (path == null) {
            Path temp = tempFileSpace.createTempFile().toPath();
            try {
                Files.write(temp, content);
            } catch (IOException ex) {
                throw new TempFileException(ex);
            }
            this.path = temp;
        }
        return path;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentAsString() {
        return new String(content);
    }

    public String getContentAsString(Charset charset) {
        return new String(content, charset);
    }

    public InputStream newContentInputStream() {
        return new ByteArrayInputStream(content);
    }
}
