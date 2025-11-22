package org.codelibs.maven.yuicompressor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Test cases for file aggregation functionality.
 */
public class AggregationTest {
    private File tempDir;
    private final DefaultBuildContext buildContext = new DefaultBuildContext();

    @Before
    public void setUp() throws IOException {
        // Use UUID to ensure uniqueness across concurrent test runs
        final String uniquePrefix = this.getClass().getSimpleName() + "-" + UUID.randomUUID();
        tempDir = Files.createTempDirectory(uniquePrefix).toFile();
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && tempDir.exists()) {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(final File directory) throws IOException {
        final File[] files = directory.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }

    @Test
    public void test0to1() throws IOException {
        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");

        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertFalse(target.output.exists());

        target.includes = new String[]{};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertFalse(target.output.exists());

        target.includes = new String[]{"**/*.js"};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertFalse(target.output.exists());
    }

    @Test
    public void test1to1() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");
        target.includes = new String[]{f1.getName()};

        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1), readFile(target.output));
    }

    @Test
    public void test2to1() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final File f2 = new File(tempDir, "02.js");
        writeFile(f2, "22\n22");

        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");

        target.includes = new String[]{f1.getName(), f2.getName()};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + readFile(f2), readFile(target.output));

        Files.delete(target.output.toPath());
        target.includes = new String[]{"*.js"};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + readFile(f2), readFile(target.output));
    }

    @Test
    public void testNoDuplicateAggregation() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final File f2 = new File(tempDir, "02.js");
        writeFile(f2, "22\n22");

        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");

        target.includes = new String[]{f1.getName(), f1.getName(), f2.getName()};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + readFile(f2), readFile(target.output));

        Files.delete(target.output.toPath());
        target.includes = new String[]{f1.getName(), "*.js"};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + readFile(f2), readFile(target.output));
    }

    @Test
    public void test2to1Order() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final File f2 = new File(tempDir, "02.js");
        writeFile(f2, "2");

        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");

        target.includes = new String[]{f2.getName(), f1.getName()};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f2) + readFile(f1), readFile(target.output));
    }

    @Test
    public void test2to1WithNewLine() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final File f2 = new File(tempDir, "02.js");
        writeFile(f2, "22\n22");

        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");
        target.insertNewLine = true;
        target.includes = new String[]{f1.getName(), f2.getName()};

        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + "\n" + readFile(f2) + "\n", readFile(target.output));
    }

    @Test
    public void testAbsolutePathFromInside() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final File f2 = new File(tempDir, "02.js");
        writeFile(f2, "22\n22");

        final Aggregation target = new Aggregation();
        target.output = new File(tempDir, "output.js");

        target.includes = new String[]{f1.getAbsolutePath(), f2.getName()};
        assertFalse(target.output.exists());
        target.run(null, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + readFile(f2), readFile(target.output));
    }

    @Test
    public void testAbsolutePathFromOutside() throws IOException {
        final File f1 = Files.createTempFile("test-01", ".js").toFile();
        try {
            writeFile(f1, "1");

            final File f2 = new File(tempDir, "02.js");
            writeFile(f2, "22\n22");

            final Aggregation target = new Aggregation();
            target.output = new File(tempDir, "output.js");

            target.includes = new String[]{f1.getAbsolutePath(), f2.getName()};
            assertFalse(target.output.exists());
            target.run(null, buildContext);
            assertTrue(target.output.exists());
            assertEquals(readFile(f1) + readFile(f2), readFile(target.output));
        } finally {
            Files.deleteIfExists(f1.toPath());
        }
    }

    @Test
    public void testAutoExcludeWildcards() throws IOException {
        final File f1 = new File(tempDir, "01.js");
        writeFile(f1, "1");

        final File f2 = new File(tempDir, "02.js");
        writeFile(f2, "22\n22");

        final Aggregation target = new Aggregation();
        target.autoExcludeWildcards = true;
        target.output = new File(tempDir, "output.js");

        final Collection<File> previouslyIncluded = new HashSet<>();
        previouslyIncluded.add(f1);

        target.includes = new String[]{f1.getName(), f2.getName()};
        assertFalse(target.output.exists());
        target.run(previouslyIncluded, buildContext);
        assertTrue(target.output.exists());
        assertEquals(readFile(f1) + readFile(f2), readFile(target.output));

        Files.delete(target.output.toPath());
        target.includes = new String[]{"*.js"};
        assertFalse(target.output.exists());
        // f1 was in previouslyIncluded so it is not included
        final List<File> result = target.run(previouslyIncluded, buildContext);
        assertEquals(1, result.size());
        // Compare canonical paths to handle platform-specific path differences (e.g., Windows 8.3 short names)
        assertEquals(f2.getCanonicalPath(), result.get(0).getCanonicalPath());
        assertTrue(target.output.exists());
        assertEquals(readFile(f2), readFile(target.output));
    }

    private void writeFile(final File file, final String content) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private String readFile(final File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
