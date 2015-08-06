/*
 * Copyright 2015 Daniel Gredler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.gredler.testfs;

import static com.sun.nio.file.SensitivityWatchEventModifier.MEDIUM;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link TestPath}.
 */
public class TestPathTest {

    private static final FileSystem DEFAULT_FS = FileSystems.getDefault();
    private static final String DIRECTORY = "src/test/resources/dir";
    private static final String SIMPLE_TXT = "src/test/resources/simple.txt";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testEqualsAndHashCodeAndCompareTo() {

        Path defaultPath1 = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path defaultPath2 = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath1 = new TestFS().create().getPath(SIMPLE_TXT);
        Path testPath2 = new TestFS().create().getPath(SIMPLE_TXT);

        assertEquals(defaultPath1, defaultPath2);
        assertEquals(defaultPath2, defaultPath1);
        assertEquals(defaultPath1.hashCode(), defaultPath2.hashCode());
        assertEquals(defaultPath2.hashCode(), defaultPath1.hashCode());
        assertEquals(0, defaultPath1.compareTo(defaultPath2));
        assertEquals(0, defaultPath2.compareTo(defaultPath1));

        assertEquals(testPath1, testPath2);
        assertEquals(testPath2, testPath1);
        assertEquals(testPath1.hashCode(), testPath2.hashCode());
        assertEquals(testPath2.hashCode(), testPath1.hashCode());
        assertEquals(0, testPath1.compareTo(testPath2));
        assertEquals(0, testPath2.compareTo(testPath1));

        assertNotEquals(testPath1, defaultPath1);
        assertNotEquals(defaultPath1, testPath1);
        assertNotEquals(testPath1.hashCode(), defaultPath1.hashCode());
        assertNotEquals(defaultPath1.hashCode(), testPath1.hashCode());

        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("cannot be cast to");

        assertNotEquals(0, testPath1.compareTo(defaultPath1));
    }

    @Test
    public void testWrapAndUnwrap() {

        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);

        TestFileSystem fs = (TestFileSystem) new TestFS().create();
        TestPath testPath = (TestPath) fs.getPath(SIMPLE_TXT);

        assertEquals(testPath, TestPath.wrap(defaultPath, fs));
        assertEquals(defaultPath, testPath.unwrap());
        assertSame(testPath, TestPath.wrap(testPath, fs));
    }

    @Test
    public void testGetFileSystem() {
        FileSystem fs = new TestFS().create();
        Path testPath = fs.getPath(SIMPLE_TXT);
        assertEquals(fs, testPath.getFileSystem());
    }

    @Test
    public void testIsAbsolute() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertFalse(path.isAbsolute());
        assertTrue(path.toAbsolutePath().isAbsolute());
    }

    @Test
    public void testGetRoot() {

        FileSystem fs = new TestFS().create();

        Path defaultPath1 = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath1 = fs.getPath(SIMPLE_TXT);
        assertNull(defaultPath1.getRoot());
        assertNull(testPath1.getRoot());

        Path defaultPath2 = DEFAULT_FS.getPath("/foo");
        Path testPath2 = fs.getPath("/foo");
        assertNotNull(defaultPath2.getRoot());
        assertNotNull(testPath2.getRoot());
    }

    @Test
    public void testGetFileName() {
        FileSystem fs = new TestFS().create();
        assertNotEquals("simple.txt", fs.getPath(SIMPLE_TXT).toString());
        assertEquals("simple.txt", fs.getPath(SIMPLE_TXT).getFileName().toString());
    }

    @Test
    public void testGetParent() {

        FileSystem fs = new TestFS().create();

        Path defaultPath1 = DEFAULT_FS.getPath("/");
        Path testPath1 = fs.getPath("/");
        assertNull(defaultPath1.getParent());
        assertNull(testPath1.getParent());

        Path defaultPath2 = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath2 = fs.getPath(SIMPLE_TXT);
        assertNotNull(defaultPath2.getParent());
        assertNotNull(testPath2.getParent());
    }

    @Test
    public void testGetNameCount() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertEquals(4, path.getNameCount());
    }

    @Test
    public void testGetName() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertEquals(fs.getPath("src"), path.getName(0));
        assertEquals(fs.getPath("test"), path.getName(1));
        assertEquals(fs.getPath("resources"), path.getName(2));
        assertEquals(fs.getPath("simple.txt"), path.getName(3));
    }

    @Test
    public void testSubpath() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertEquals(fs.getPath("src/test"), path.subpath(0, 2));
    }

    @Test
    public void testStartsWithPath() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b/c/d");
        Path path2 = fs.getPath("a", "b", "c");
        Path path3 = fs.getPath("a", "f");
        assertTrue(path.startsWith(path2));
        assertFalse(path.startsWith(path3));
    }

    @Test
    public void testStartsWithString() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b/c/d");
        assertTrue(path.startsWith("a/b/c"));
        assertFalse(path.startsWith("a/f"));
    }

    @Test
    public void testEndsWithPath() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b/c/d");
        Path path2 = fs.getPath("b", "c", "d");
        Path path3 = fs.getPath("c", "f");
        assertTrue(path.endsWith(path2));
        assertFalse(path.endsWith(path3));
    }

    @Test
    public void testEndsWithString() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b/c/d");
        assertTrue(path.endsWith("b/c/d"));
        assertFalse(path.endsWith("c/f"));
    }

    @Test
    public void testNormalize() {
        FileSystem fs = new TestFS().create();
        Path path1 = fs.getPath("a/b/c/d");
        Path path2 = fs.getPath("a/b/../b/c/d");
        Path path3 = fs.getPath("a/b/./c/d");
        assertNotEquals(path1, path2);
        assertNotEquals(path1, path3);
        assertEquals(path1, path2.normalize());
        assertEquals(path1, path3.normalize());
    }

    @Test
    public void testResolvePath() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b");
        Path path2 = fs.getPath("c", "d");
        Path path3 = fs.getPath("/");
        assertEquals(fs.getPath("a/b/c/d"), path.resolve(path2));
        assertEquals(fs.getPath("/"), path.resolve(path3));
    }

    @Test
    public void testResolveString() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b");
        assertEquals(fs.getPath("a/b/c/d"), path.resolve("c/d"));
        assertEquals(fs.getPath("/"), path.resolve("/"));
    }

    @Test
    public void testResolveSiblingPath() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b");
        Path path2 = fs.getPath("c", "d");
        Path path3 = fs.getPath("/");
        assertEquals(fs.getPath("a/c/d"), path.resolveSibling(path2));
        assertEquals(fs.getPath("/"), path.resolveSibling(path3));
    }

    @Test
    public void testResolveSiblingString() {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath("a/b");
        assertEquals(fs.getPath("a/c/d"), path.resolveSibling("c/d"));
        assertEquals(fs.getPath("/"), path.resolveSibling("/"));
    }

    @Test
    public void testRelativize() {
        FileSystem fs = new TestFS().create();
        Path ab = fs.getPath("a/b");
        Path abcd = fs.getPath("a/b/c/d");
        Path cd = fs.getPath("c/d");
        assertEquals(cd, ab.relativize(abcd));
    }

    @Test
    public void testToUri() {
        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath = new TestFS().create().getPath(SIMPLE_TXT);
        assertEquals(defaultPath.toUri(), testPath.toUri());
    }

    @Test
    public void testToAbsolutePath() {
        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath = new TestFS().create().getPath(SIMPLE_TXT);
        assertEquals(defaultPath.toAbsolutePath().toString(), testPath.toAbsolutePath().toString());
    }

    @Test
    public void testToRealPath() throws IOException {
        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath = new TestFS().create().getPath(SIMPLE_TXT);
        assertEquals(defaultPath.toRealPath().toString(), testPath.toRealPath().toString());
    }

    @Test
    public void testToFile() {
        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);
        Path testPath = new TestFS().create().getPath(SIMPLE_TXT);
        assertEquals(defaultPath.toFile(), testPath.toFile());
    }

    @Test
    public void testRegisterWatch() throws IOException {
        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(DIRECTORY);
        WatchService watcher = fs.newWatchService();
        assertNotNull(path.register(watcher, ENTRY_CREATE));
        assertNotNull(path.register(watcher, new WatchEvent.Kind[] { ENTRY_CREATE }, MEDIUM));
    }

    @Test
    public void testIterator() {

        FileSystem fs = new TestFS().create();

        Iterator< Path > defaultElements = DEFAULT_FS.getPath(SIMPLE_TXT).iterator();
        Iterator< Path > testElements = fs.getPath(SIMPLE_TXT).iterator();

        while (defaultElements.hasNext() && testElements.hasNext()) {
            Path defaultElement = defaultElements.next();
            Path testElement = testElements.next();
            assertEquals(defaultElement.toString(), testElement.toString());
            assertEquals(DEFAULT_FS, defaultElement.getFileSystem());
            assertEquals(fs, testElement.getFileSystem());
        }

        assertFalse(defaultElements.hasNext());
        assertFalse(testElements.hasNext());
    }
}
