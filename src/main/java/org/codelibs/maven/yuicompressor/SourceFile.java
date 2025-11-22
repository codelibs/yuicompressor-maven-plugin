package org.codelibs.maven.yuicompressor;

import java.io.File;
import java.util.Objects;

/**
 * Represents a source file with its source and destination roots.
 * Handles file path resolution and destination file creation.
 */
public class SourceFile {

    private final File srcRoot;
    private final File destRoot;
    private final boolean destAsSource;
    private final String rpath;
    private final String extension;

    /**
     * Creates a new SourceFile instance.
     *
     * @param srcRoot      the source root directory
     * @param destRoot     the destination root directory
     * @param name         the relative file name
     * @param destAsSource whether to use destination as source if available
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    public SourceFile(final File srcRoot, final File destRoot, final String name, final boolean destAsSource) {
        this.srcRoot = Objects.requireNonNull(srcRoot, "srcRoot must not be null");
        this.destRoot = Objects.requireNonNull(destRoot, "destRoot must not be null");
        this.destAsSource = destAsSource;

        Objects.requireNonNull(name, "name must not be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }

        final int sep = name.lastIndexOf('.');
        if (sep > 0) {
            this.extension = name.substring(sep);
            this.rpath = name.substring(0, sep);
        } else {
            this.extension = "";
            this.rpath = name;
        }
    }

    /**
     * Returns the source file. If destAsSource is true and destination file exists,
     * returns the destination file instead.
     *
     * @return the file to be used as source
     */
    public File toFile() {
        final String frpath = rpath + extension;
        if (destAsSource) {
            final File defaultDest = new File(destRoot, frpath);
            if (defaultDest.exists() && defaultDest.canRead()) {
                return defaultDest;
            }
        }
        return new File(srcRoot, frpath);
    }

    /**
     * Creates a destination file with the given suffix.
     *
     * @param suffix the suffix to append before the extension
     * @return the destination file
     */
    public File toDestFile(final String suffix) {
        Objects.requireNonNull(suffix, "suffix must not be null");
        return new File(destRoot, rpath + suffix + extension);
    }

    /**
     * Returns the file extension including the dot.
     *
     * @return the file extension (e.g., ".js", ".css") or empty string if no extension
     */
    public String getExtension() {
        return extension;
    }
}
