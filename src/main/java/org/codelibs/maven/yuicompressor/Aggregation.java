package org.codelibs.maven.yuicompressor;

import org.codehaus.plexus.util.DirectoryScanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Handles aggregation (concatenation) of multiple files into a single output file.
 * Supports file inclusion/exclusion patterns, file headers, and automatic cleanup.
 */
public class Aggregation {
    public File inputDir;
    public File output;
    public String[] includes;
    public String[] excludes;
    public boolean removeIncluded = false;
    public boolean insertNewLine = false;
    public boolean insertFileHeader = false;
    public boolean fixLastSemicolon = false;
    public boolean autoExcludeWildcards = false;

    /**
     * Runs the aggregation process.
     *
     * @param previouslyIncludedFiles files to exclude if autoExcludeWildcards is enabled
     * @param buildContext            build context for output stream creation
     * @return list of files that were aggregated
     * @throws IOException if file operations fail
     */
    public List<File> run(final Collection<File> previouslyIncludedFiles, final BuildContext buildContext) throws IOException {
        return this.run(previouslyIncludedFiles, buildContext, null);
    }

    /**
     * Runs the aggregation process with incremental build support.
     *
     * @param previouslyIncludedFiles files to exclude if autoExcludeWildcards is enabled
     * @param buildContext            build context for output stream creation
     * @param incrementalFiles        set of modified files in incremental build
     * @return list of files that were aggregated
     * @throws IOException if file operations fail
     */
    public List<File> run(final Collection<File> previouslyIncludedFiles, final BuildContext buildContext,
                          final Set<String> incrementalFiles) throws IOException {
        Objects.requireNonNull(buildContext, "buildContext must not be null");

        defineInputDir();

        final List<File> files = autoExcludeWildcards
                ? getIncludedFiles(previouslyIncludedFiles, buildContext, incrementalFiles)
                : getIncludedFiles(null, buildContext, incrementalFiles);

        if (!files.isEmpty()) {
            aggregateFiles(files, buildContext);
        }

        return files;
    }

    private void aggregateFiles(final List<File> files, final BuildContext buildContext) throws IOException {
        output = output.getCanonicalFile();

        if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            throw new IOException("Failed to create output directory: " + output.getParentFile());
        }

        try (OutputStream out = buildContext.newFileOutputStream(output)) {
            for (final File file : files) {
                if (file.getCanonicalPath().equals(output.getCanonicalPath())) {
                    continue; // Skip output file if it's in the include list
                }

                writeFileToOutput(file, out, buildContext);
            }
        }
    }

    private void writeFileToOutput(final File file, final OutputStream out, final BuildContext buildContext) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            if (insertFileHeader) {
                out.write(createFileHeader(file).getBytes(StandardCharsets.UTF_8));
            }

            in.transferTo(out);

            if (fixLastSemicolon) {
                out.write(';');
            }
            if (insertNewLine) {
                out.write('\n');
            }
        }

        if (removeIncluded) {
            if (!file.delete()) {
                throw new IOException("Failed to delete file after aggregation: " + file);
            }
            buildContext.refresh(file);
        }
    }

    private String createFileHeader(final File file) {
        final StringBuilder header = new StringBuilder();
        header.append("/*").append(file.getName()).append("*/");

        if (insertNewLine) {
            header.append('\n');
        }

        return header.toString();
    }

    private void defineInputDir() throws IOException {
        if (inputDir == null) {
            inputDir = output.getParentFile();
        }
        inputDir = inputDir.getCanonicalFile();
        if (!inputDir.isDirectory()) {
            throw new IOException("Input directory not found or not a directory: " + inputDir);
        }
    }

    private List<File> getIncludedFiles(final Collection<File> previouslyIncludedFiles, final BuildContext buildContext,
                                        final Set<String> incrementalFiles) throws IOException {
        final List<File> filesToAggregate = new ArrayList<>();

        if (includes != null) {
            for (final String include : includes) {
                addInto(include, filesToAggregate, previouslyIncludedFiles);
            }
        }

        // If build is incremental with no delta, then don't include for aggregation
        if (buildContext.isIncremental()) {
            if (incrementalFiles != null) {
                boolean aggregateMustBeUpdated = false;
                for (final File file : filesToAggregate) {
                    if (incrementalFiles.contains(file.getAbsolutePath())) {
                        aggregateMustBeUpdated = true;
                        break;
                    }
                }

                if (aggregateMustBeUpdated) {
                    return filesToAggregate;
                }
            }
            return new ArrayList<>();
        }

        return filesToAggregate;
    }

    private void addInto(final String include, final List<File> includedFiles,
                         final Collection<File> previouslyIncludedFiles) throws IOException {
        if (include.contains("*")) {
            addWildcardFiles(include, includedFiles, previouslyIncludedFiles);
        } else {
            addSingleFile(include, includedFiles);
        }
    }

    private void addWildcardFiles(final String include, final List<File> includedFiles,
                                  final Collection<File> previouslyIncludedFiles) throws IOException {
        final DirectoryScanner scanner = newScanner();
        scanner.setIncludes(new String[]{include});
        scanner.scan();

        final String[] rpaths = scanner.getIncludedFiles();
        Arrays.sort(rpaths);

        for (final String rpath : rpaths) {
            final File file = new File(scanner.getBasedir(), rpath);
            if (!includedFiles.contains(file) &&
                    (previouslyIncludedFiles == null || !previouslyIncludedFiles.contains(file))) {
                includedFiles.add(file);
            }
        }
    }

    private void addSingleFile(final String include, final List<File> includedFiles) {
        File file = new File(include);
        if (!file.isAbsolute()) {
            file = new File(inputDir, include);
        }
        if (!includedFiles.contains(file)) {
            includedFiles.add(file);
        }
    }

    private DirectoryScanner newScanner() throws IOException {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(inputDir);

        if (excludes != null && excludes.length > 0) {
            scanner.setExcludes(excludes);
        }

        scanner.addDefaultExcludes();
        return scanner;
    }
}
