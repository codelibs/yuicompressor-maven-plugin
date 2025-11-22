package org.codelibs.maven.yuicompressor;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;

/**
 * Check JavaScript files with JSLint.
 *
 * @author David Bernard
 * @author CodeLibs Project
 */
@Mojo(name = "jslint", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class JSLintMojo extends MojoSupport {
    private JSLintChecker jslint;

    @Override
    protected String[] getDefaultIncludes() {
        return new String[]{"**/*.js"};
    }

    @Override
    public void beforeProcess() throws IOException {
        jslint = new JSLintChecker();
    }

    @Override
    public void afterProcess() {
        // No cleanup needed
    }

    @Override
    protected void processFile(final SourceFile src) throws IOException {
        getLog().info("Checking file: " + src.toFile());
        jslint.check(src.toFile(), jsErrorReporter);
    }
}
