package org.codelibs.maven.yuicompressor;

import org.apache.maven.plugin.logging.Log;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Objects;

/**
 * Error reporter implementation for Maven Mojo that integrates with Maven logging
 * and BuildContext for IDE incremental build support.
 */
public class ErrorReporter4Mojo implements ErrorReporter {

    private String defaultFilename;
    private final boolean acceptWarn;
    private final Log log;
    private int warningCount;
    private int errorCount;
    private final BuildContext buildContext;
    private File sourceFile;

    /**
     * Creates a new error reporter.
     *
     * @param log          Maven logger
     * @param jswarn       whether to accept and report warnings
     * @param buildContext build context for IDE integration
     */
    public ErrorReporter4Mojo(final Log log, final boolean jswarn, final BuildContext buildContext) {
        this.log = Objects.requireNonNull(log, "log must not be null");
        this.acceptWarn = jswarn;
        this.buildContext = Objects.requireNonNull(buildContext, "buildContext must not be null");
    }

    /**
     * Sets the default filename to use when source name is not provided.
     *
     * @param filename the default filename
     */
    public void setDefaultFileName(final String filename) {
        this.defaultFilename = (filename == null || filename.isBlank()) ? null : filename;
    }

    /**
     * Returns the number of errors reported.
     *
     * @return error count
     */
    public int getErrorCnt() {
        return errorCount;
    }

    /**
     * Returns the number of warnings reported.
     *
     * @return warning count
     */
    public int getWarningCnt() {
        return warningCount;
    }

    /**
     * Sets the current source file being processed.
     *
     * @param file the source file
     */
    public void setFile(final File file) {
        this.sourceFile = file;
    }

    @Override
    public void error(final String message, final String sourceName, final int line,
                      final String lineSource, final int lineOffset) {
        final String fullMessage = newMessage(message, sourceName, line, lineSource, lineOffset);
        buildContext.addMessage(sourceFile, line, lineOffset, message, BuildContext.SEVERITY_ERROR, null);
        log.error(fullMessage);
        errorCount++;
    }

    @Override
    public EvaluatorException runtimeError(final String message, final String sourceName, final int line,
                                           final String lineSource, final int lineOffset) {
        error(message, sourceName, line, lineSource, lineOffset);
        throw new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
    }

    @Override
    public void warning(final String message, final String sourceName, final int line,
                        final String lineSource, final int lineOffset) {
        if (acceptWarn) {
            final String fullMessage = newMessage(message, sourceName, line, lineSource, lineOffset);
            buildContext.addMessage(sourceFile, line, lineOffset, message, BuildContext.SEVERITY_WARNING, null);
            log.warn(fullMessage);
            warningCount++;
        }
    }

    private String newMessage(final String message, String sourceName, final int line,
                              final String lineSource, final int lineOffset) {
        final StringBuilder back = new StringBuilder();

        if (sourceName == null || sourceName.isBlank()) {
            sourceName = defaultFilename;
        }

        if (sourceName != null) {
            back.append(sourceName)
                    .append(":line ")
                    .append(line)
                    .append(":column ")
                    .append(lineOffset)
                    .append(':');
        }

        if (message != null && !message.isBlank()) {
            back.append(message);
        } else {
            back.append("unknown error");
        }

        if (lineSource != null && !lineSource.isBlank()) {
            back.append("\n\t").append(lineSource);
        }

        return back.toString();
    }
}
