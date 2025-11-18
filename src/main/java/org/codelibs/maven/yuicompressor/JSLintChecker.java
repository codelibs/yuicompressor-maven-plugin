package org.codelibs.maven.yuicompressor;

import org.mozilla.javascript.ErrorReporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * JSLint checker that validates JavaScript files using the embedded JSLint library.
 */
class JSLintChecker {
    private final String jslintPath;

    /**
     * Creates a new JSLint checker by extracting the JSLint library to a temporary file.
     *
     * @throws IOException if unable to extract or create temporary JSLint file
     */
    public JSLintChecker() throws IOException {
        final Path jslint = Files.createTempFile("jslint", ".js");

        try (InputStream in = getClass().getResourceAsStream("/jslint.js")) {
            if (in == null) {
                throw new IOException("JSLint resource '/jslint.js' not found in classpath");
            }

            try (FileOutputStream out = new FileOutputStream(jslint.toFile())) {
                in.transferTo(out);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(jslint);
            } catch (IOException ignored) {
                // Ignore cleanup errors
            }
            throw new IOException("Failed to extract JSLint resource", e);
        }

        jslint.toFile().deleteOnExit();
        this.jslintPath = jslint.toAbsolutePath().toString();
    }

    /**
     * Checks a JavaScript file using JSLint.
     *
     * @param jsFile   the JavaScript file to check
     * @param reporter error reporter for reporting issues
     * @throws IllegalArgumentException if jsFile is null
     */
    public void check(final File jsFile, final ErrorReporter reporter) {
        Objects.requireNonNull(jsFile, "jsFile must not be null");
        Objects.requireNonNull(reporter, "reporter must not be null");

        final String[] args = new String[]{jslintPath, jsFile.getAbsolutePath()};
        BasicRhinoShell.exec(args, reporter);
    }
}
