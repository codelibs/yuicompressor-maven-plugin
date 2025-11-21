package org.codelibs.maven.yuicompressor;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * Apply YUI Compressor compression on JavaScript and CSS files.
 *
 * @author David Bernard
 * @author CodeLibs Project
 */
@Mojo(name = "compress", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class YuiCompressorMojo extends MojoSupport {

    /**
     * Read the input file using this encoding.
     */
    @Parameter(property = "file.encoding", defaultValue = "UTF-8")
    private String encoding;

    /**
     * The output filename suffix.
     */
    @Parameter(property = "maven.yuicompressor.suffix", defaultValue = "-min")
    private String suffix;

    /**
     * If no suffix should be added to output filename.
     */
    @Parameter(property = "maven.yuicompressor.nosuffix", defaultValue = "false")
    private boolean nosuffix;

    /**
     * Insert line breaks in output after the specified column number.
     */
    @Parameter(property = "maven.yuicompressor.linebreakpos", defaultValue = "-1")
    private int linebreakpos;

    /**
     * [js only] Skip compression (copy only).
     */
    @Parameter(property = "maven.yuicompressor.nocompress", defaultValue = "false")
    private boolean nocompress;

    /**
     * [js only] Minify only, do not obfuscate.
     */
    @Parameter(property = "maven.yuicompressor.nomunge", defaultValue = "false")
    private boolean nomunge;

    /**
     * [js only] Preserve unnecessary semicolons.
     */
    @Parameter(property = "maven.yuicompressor.preserveAllSemiColons", defaultValue = "false")
    private boolean preserveAllSemiColons;

    /**
     * [js only] Disable all micro optimizations.
     */
    @Parameter(property = "maven.yuicompressor.disableOptimizations", defaultValue = "false")
    private boolean disableOptimizations;

    /**
     * Force compression of all files, even if compressed file is newer than source.
     */
    @Parameter(property = "maven.yuicompressor.force", defaultValue = "false")
    private boolean force;

    /**
     * List of aggregation/concatenation to do after processing.
     */
    @Parameter
    private Aggregation[] aggregations;

    /**
     * Create gzipped versions of compressed files.
     */
    @Parameter(property = "maven.yuicompressor.gzip", defaultValue = "false")
    private boolean gzip;

    /**
     * GZIP compression level (0-9).
     */
    @Parameter(property = "maven.yuicompressor.level", defaultValue = "9")
    private int level;

    /**
     * Show compression statistics.
     */
    @Parameter(property = "maven.yuicompressor.statistics", defaultValue = "true")
    private boolean statistics;

    /**
     * Aggregate files before minification.
     */
    @Parameter(property = "maven.yuicompressor.preProcessAggregates", defaultValue = "false")
    private boolean preProcessAggregates;

    /**
     * Use the input file as output when the compressed file is larger than the original.
     */
    @Parameter(property = "maven.yuicompressor.useSmallestFile", defaultValue = "true")
    private boolean useSmallestFile;

    private long inSizeTotal;
    private long outSizeTotal;

    /**
     * Track updated files for aggregation on incremental builds.
     */
    private Set<String> incrementalFiles;

    @Override
    protected String[] getDefaultIncludes() {
        return new String[]{"**/*.css", "**/*.js"};
    }

    @Override
    public void beforeProcess() throws IOException {
        if (nosuffix) {
            suffix = "";
        }

        if (preProcessAggregates) {
            aggregate();
        }
    }

    @Override
    protected void afterProcess() throws IOException {
        if (statistics && (inSizeTotal > 0)) {
            getLog().info(String.format("Total: input (%db) -> output (%db) [%d%%]",
                    inSizeTotal, outSizeTotal, (outSizeTotal * 100) / inSizeTotal));
        }

        if (!preProcessAggregates) {
            aggregate();
        }
    }

    private void aggregate() throws IOException {
        if (aggregations == null) {
            return;
        }

        final Set<File> previouslyIncludedFiles = new HashSet<>();
        for (final Aggregation aggregation : aggregations) {
            getLog().info("Generating aggregation: " + aggregation.output);
            final Collection<File> aggregatedFiles = aggregation.run(previouslyIncludedFiles, buildContext, incrementalFiles);
            previouslyIncludedFiles.addAll(aggregatedFiles);

            final File gzipped = gzipIfRequested(aggregation.output);
            if (statistics) {
                logAggregationStatistics(aggregation, gzipped);
            }
        }
    }

    private void logAggregationStatistics(final Aggregation aggregation, final File gzipped) throws IOException {
        if (gzipped != null) {
            getLog().info(String.format("%s (%db) -> %s (%db) [%d%%]",
                    aggregation.output.getName(), aggregation.output.length(),
                    gzipped.getName(), gzipped.length(),
                    ratioOfSize(aggregation.output, gzipped)));
        } else if (aggregation.output.exists()) {
            getLog().info(String.format("%s (%db)",
                    aggregation.output.getName(), aggregation.output.length()));
        } else {
            getLog().warn(String.format("%s not created", aggregation.output.getName()));
        }
    }

    @Override
    protected void processFile(final SourceFile src) throws IOException {
        final File inFile = src.toFile();

        if (buildContext.isIncremental()) {
            if (!buildContext.hasDelta(inFile)) {
                getLog().info("Skipping " + inFile + " (no delta)");
                return;
            }
            if (incrementalFiles == null) {
                incrementalFiles = new HashSet<>();
            }
        }

        getLog().debug("Compressing: " + src.toFile() + " -> " + src.toDestFile(suffix));

        final File outFile = src.toDestFile(suffix);

        if (isMinifiedFile(inFile)) {
            getLog().debug("Skipping already minified file: " + inFile);
            return;
        }

        if (minifiedFileExistsInSource(inFile, outFile)) {
            getLog().info("Compressed file already exists in source directory: " + outFile.getAbsolutePath());
            return;
        }

        if (!force && outFile.exists() && (outFile.lastModified() > inFile.lastModified())) {
            getLog().info("Output file is newer than input, skipping: " + outFile);
            return;
        }

        compressFile(src, inFile, outFile);
    }

    private void compressFile(final SourceFile src, final File inFile, final File outFile) throws IOException {
        final File outFileTmp = new File(outFile.getAbsolutePath() + ".tmp");
        Files.deleteIfExists(outFileTmp.toPath());

        final Charset charset = Charset.forName(encoding);

        try (InputStreamReader in = new InputStreamReader(new FileInputStream(inFile), charset);
             OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFileTmp), charset)) {

            if (!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs()) {
                throw new IOException("Cannot create output directory: " + outFile.getParentFile());
            }

            if (nocompress) {
                getLog().info("Compression disabled, copying: " + inFile.getName());
                in.transferTo(out);
            } else if (".js".equalsIgnoreCase(src.getExtension())) {
                compressJavaScript(in, out);
            } else if (".css".equalsIgnoreCase(src.getExtension())) {
                compressCss(in, out);
            }
        }

        finalizeOutput(inFile, outFile, outFileTmp);
    }

    private void compressJavaScript(final InputStreamReader in, final OutputStreamWriter out) throws IOException {
        final JavaScriptCompressor compressor = new JavaScriptCompressor(in, jsErrorReporter);
        compressor.compress(out, linebreakpos, !nomunge, jswarn, preserveAllSemiColons, disableOptimizations);
    }

    private void compressCss(final InputStreamReader in, final OutputStreamWriter out) throws IOException {
        try {
            final CssCompressor compressor = new CssCompressor(in);
            compressor.compress(out, linebreakpos);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid characters or malformed syntax in CSS file", e);
        }
    }

    private void finalizeOutput(final File inFile, final File outFile, final File outFileTmp) throws IOException {
        final boolean useOriginal = useSmallestFile && inFile.length() < outFileTmp.length();

        if (useOriginal) {
            Files.delete(outFileTmp.toPath());
            Files.copy(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLog().debug("Compressed output larger than input, using original");
        } else {
            Files.move(outFileTmp.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            buildContext.refresh(outFile);
        }

        if (buildContext.isIncremental()) {
            incrementalFiles.add(outFile.getAbsolutePath());
        }

        final File gzipped = gzipIfRequested(outFile);

        if (statistics) {
            logFileStatistics(inFile, outFile, gzipped, useOriginal);
        }
    }

    private void logFileStatistics(final File inFile, final File outFile, final File gzipped, final boolean useOriginal)
            throws IOException {
        inSizeTotal += inFile.length();
        outSizeTotal += outFile.length();

        final String fileStats;
        if (useOriginal) {
            fileStats = String.format("%s (%db) -> %s (%db) [original used - compressed was larger]",
                    inFile.getName(), inFile.length(), outFile.getName(), outFile.length());
        } else {
            fileStats = String.format("%s (%db) -> %s (%db) [%d%%]",
                    inFile.getName(), inFile.length(), outFile.getName(), outFile.length(),
                    ratioOfSize(inFile, outFile));
        }

        if (gzipped != null) {
            getLog().info(fileStats + String.format(" -> %s (%db) [%d%%]",
                    gzipped.getName(), gzipped.length(), ratioOfSize(inFile, gzipped)));
        } else {
            getLog().info(fileStats);
        }
    }

    protected File gzipIfRequested(final File file) throws IOException {
        if (!gzip || file == null || !file.exists()) {
            return null;
        }

        if (".gz".equalsIgnoreCase(getExtension(file.getName()))) {
            return null;
        }

        final File gzipped = new File(file.getAbsolutePath() + ".gz");
        getLog().debug("Creating gzip version: " + gzipped.getName());

        try (FileInputStream in = new FileInputStream(file);
             GZIPOutputStream out = createGzipOutputStream(gzipped)) {
            in.transferTo(out);
        }

        return gzipped;
    }

    private GZIPOutputStream createGzipOutputStream(final File gzipped) throws IOException {
        final OutputStream baseOut = buildContext.newFileOutputStream(gzipped);
        // Use anonymous class to set compression level without reflection
        return new GZIPOutputStream(baseOut) {
            {
                def.setLevel(level);
            }
        };
    }

    protected long ratioOfSize(final File file100, final File fileX) {
        final long v100 = Math.max(file100.length(), 1);
        final long vX = Math.max(fileX.length(), 1);
        return (vX * 100) / v100;
    }

    private boolean isMinifiedFile(final File inFile) {
        // When suffix is empty (nosuffix=true), we cannot determine if a file is minified by its name
        if (suffix == null || suffix.isEmpty()) {
            return false;
        }
        final String filename = inFile.getName().toLowerCase();
        return filename.endsWith(suffix + ".js") || filename.endsWith(suffix + ".css");
    }

    private static boolean minifiedFileExistsInSource(final File source, final File dest) {
        final String parent = source.getParent();
        final String destFilename = dest.getName();
        final File file = new File(parent, destFilename);
        return file.exists();
    }

    private String getExtension(final String filename) {
        final int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }
}
