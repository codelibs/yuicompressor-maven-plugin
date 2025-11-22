package org.codelibs.maven.yuicompressor;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for YUI Compressor Maven plugin mojos.
 * Provides common functionality for file scanning and processing.
 *
 * @author David Bernard
 * @author CodeLibs Project
 */
public abstract class MojoSupport extends AbstractMojo {
    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * Javascript source directory. (result will be put to outputDirectory).
     * This allows projects with "src/main/js" structure.
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../js")
    private File sourceDirectory;

    /**
     * Single directory for extra files to include in the WAR.
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp")
    private File warSourceDirectory;

    /**
     * The directory where the webapp is built.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File webappDirectory;

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(property = "project.build.outputDirectory")
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     */
    @Parameter(property = "project.resources")
    private List<Resource> resources;

    /**
     * List of additional excludes.
     */
    @Parameter
    private List<String> excludes;

    /**
     * Use processed resources if available.
     */
    @Parameter(defaultValue = "false")
    private boolean useProcessedResources;

    /**
     * List of additional includes.
     */
    @Parameter
    private List<String> includes;

    /**
     * Excludes files from webapp directory.
     */
    @Parameter(defaultValue = "false")
    private boolean excludeWarSourceDirectory = false;

    /**
     * Excludes files from resources directories.
     */
    @Parameter(defaultValue = "false")
    private boolean excludeResources = false;

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    /**
     * [js only] Display possible errors in the code.
     */
    @Parameter(property = "maven.yuicompressor.jswarn", defaultValue = "true")
    protected boolean jswarn;

    /**
     * Whether to skip execution.
     */
    @Parameter(property = "maven.yuicompressor.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Define if plugin must stop/fail on warnings.
     */
    @Parameter(property = "maven.yuicompressor.failOnWarning", defaultValue = "false")
    protected boolean failOnWarning;

    @Component
    protected BuildContext buildContext;

    protected ErrorReporter4Mojo jsErrorReporter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (skip) {
                getLog().debug("Execution of yuicompressor-maven-plugin skipped");
                return;
            }

            if (failOnWarning) {
                jswarn = true;
            }

            jsErrorReporter = new ErrorReporter4Mojo(getLog(), jswarn, buildContext);

            beforeProcess();

            processDir(sourceDirectory, outputDirectory, null, useProcessedResources);

            if (!excludeResources) {
                for (final Resource resource : resources) {
                    File destRoot = outputDirectory;
                    if (resource.getTargetPath() != null) {
                        destRoot = new File(outputDirectory, resource.getTargetPath());
                    }
                    processDir(new File(resource.getDirectory()), destRoot, resource.getExcludes(), useProcessedResources);
                }
            }

            if (!excludeWarSourceDirectory) {
                processDir(warSourceDirectory, webappDirectory, null, useProcessedResources);
            }

            afterProcess();

            getLog().info(String.format("Warnings: %d, Errors: %d",
                    jsErrorReporter.getWarningCnt(), jsErrorReporter.getErrorCnt()));

            if (failOnWarning && (jsErrorReporter.getWarningCnt() > 0)) {
                throw new MojoFailureException("Warnings detected and failOnWarning is enabled (see log)");
            }
        } catch (RuntimeException | MojoFailureException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MojoExecutionException("Execution failed: " + exc.getMessage(), exc);
        }
    }

    /**
     * Returns the default file includes pattern.
     *
     * @return array of include patterns
     * @throws IOException if an error occurs
     */
    protected abstract String[] getDefaultIncludes() throws IOException;

    /**
     * Called before processing starts.
     *
     * @throws IOException if an error occurs
     */
    protected abstract void beforeProcess() throws IOException;

    /**
     * Called after all processing is complete.
     *
     * @throws IOException if an error occurs
     */
    protected abstract void afterProcess() throws IOException;

    /**
     * Processes a single file.
     *
     * @param src the source file to process
     * @throws IOException if an error occurs
     */
    protected abstract void processFile(SourceFile src) throws IOException;

    /**
     * Force to use defaultIncludes (ignore srcIncludes) to avoid processing resources/includes
     * from other types than *.css or *.js.
     *
     * @see <a href="https://github.com/davidB/yuicompressor-maven-plugin/issues/19">Issue #19</a>
     */
    private void processDir(final File srcRoot, final File destRoot, final List<String> srcExcludes,
                            final boolean destAsSource) throws IOException, MojoFailureException {
        if (srcRoot == null) {
            return;
        }

        if (!srcRoot.exists()) {
            buildContext.addMessage(srcRoot, 0, 0,
                    "Directory " + srcRoot.getPath() + " does not exist",
                    BuildContext.SEVERITY_WARNING, null);
            getLog().info("Directory " + srcRoot.getPath() + " does not exist");
            return;
        }

        if (destRoot == null) {
            throw new MojoFailureException("Destination directory for " + srcRoot + " is null");
        }

        final Scanner scanner = buildContext.isIncremental()
                ? buildContext.newScanner(srcRoot)
                : createDirectoryScanner(srcRoot);

        configureScanner(scanner, srcExcludes);

        scanner.scan();

        final String[] includedFiles = scanner.getIncludedFiles();
        if (includedFiles == null || includedFiles.length == 0) {
            if (buildContext.isIncremental()) {
                getLog().info("No files have changed, skipping processing");
            } else {
                getLog().info("No files to process");
            }
            return;
        }

        for (final String name : includedFiles) {
            final SourceFile src = new SourceFile(srcRoot, destRoot, name, destAsSource);
            jsErrorReporter.setDefaultFileName(getShortFileName(src));
            jsErrorReporter.setFile(src.toFile());
            processFile(src);
        }
    }

    private Scanner createDirectoryScanner(final File srcRoot) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(srcRoot);
        return scanner;
    }

    private void configureScanner(final Scanner scanner, final List<String> srcExcludes) throws IOException {
        if (includes == null) {
            scanner.setIncludes(getDefaultIncludes());
        } else {
            scanner.setIncludes(includes.toArray(new String[0]));
        }

        // Combine source excludes and global excludes
        final List<String> allExcludes = new ArrayList<>();
        if (srcExcludes != null && !srcExcludes.isEmpty()) {
            allExcludes.addAll(srcExcludes);
        }
        if (excludes != null && !excludes.isEmpty()) {
            allExcludes.addAll(excludes);
        }

        if (!allExcludes.isEmpty()) {
            scanner.setExcludes(allExcludes.toArray(EMPTY_STRING_ARRAY));
        }

        scanner.addDefaultExcludes();
    }

    private String getShortFileName(final SourceFile src) {
        final String absolutePath = src.toFile().getAbsolutePath();
        final int lastSep = Math.max(
                absolutePath.lastIndexOf('/'),
                absolutePath.lastIndexOf(File.separatorChar)
        );
        return "..." + absolutePath.substring(lastSep + 1);
    }
}
