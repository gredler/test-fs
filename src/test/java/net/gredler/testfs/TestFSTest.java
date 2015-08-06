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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link TestFS}.
 */
public class TestFSTest {

    private static final FileSystem DEFAULT_FS = FileSystems.getDefault();
    private static final String DIRECTORY = "src/test/resources/dir";
    private static final String DIRECTORY_FILE1 = "src/test/resources/dir/file1.txt";
    private static final String DIRECTORY_FILE2 = "src/test/resources/dir/file2.txt";
    private static final String SIMPLE_TXT = "src/test/resources/simple.txt";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testCreate() {
        FileSystem fs1 = new TestFS().create();
        FileSystem fs2 = new TestFS().gitErDone();
        assertEquals(fs1, fs2);
    }

    @Test
    public void testGetScheme() {
        String expected = DEFAULT_FS.provider().getScheme();
        String actual = new TestFS().create().provider().getScheme();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetFileSystem() throws URISyntaxException {

        URI uri = new URI("file:///");
        FileSystem testFs = new TestFS().create();

        assertEquals(DEFAULT_FS, DEFAULT_FS.provider().getFileSystem(uri));
        assertEquals(DEFAULT_FS.hashCode(), DEFAULT_FS.provider().getFileSystem(uri).hashCode());

        assertEquals(testFs, testFs.provider().getFileSystem(uri));
        assertEquals(testFs.hashCode(), testFs.provider().getFileSystem(uri).hashCode());

        assertNotEquals(DEFAULT_FS, testFs);
        assertNotEquals(testFs, DEFAULT_FS);
    }

    @Test
    public void testGetPath() throws URISyntaxException {
        URI uri = new URI("file:///");
        Path defaultPath = DEFAULT_FS.provider().getPath(uri);
        Path testPath = new TestFS().create().provider().getPath(uri);
        assertEquals(defaultPath.toString(), testPath.toString());
    }

    @Test
    public void testAddFileMappingToInexistentFile() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("bar must exist, but does not");

        new TestFS().addingFile("foo", "bar");
    }

    @Test
    public void testReadFileMatchesDefaultFileSystem() throws IOException {

        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);
        assertEquals("abc", Files.readAllLines(defaultPath).get(0));

        FileSystem fs = new TestFS().create();
        Path testPath = fs.getPath(SIMPLE_TXT);
        assertEquals("abc", Files.readAllLines(testPath).get(0));
    }

    @Test
    public void testReadDelegatedFile() throws IOException {

        String inexistent = "aaaaa";
        Path defaultPath = DEFAULT_FS.getPath(inexistent);
        assertFalse(Files.exists(defaultPath));

        FileSystem fs = new TestFS().addingFile(inexistent, SIMPLE_TXT).create();
        Path testPath = fs.getPath(inexistent);
        assertEquals("abc", Files.readAllLines(testPath).get(0));
    }

    @Test
    public void testReadRemovedFile() throws IOException {

        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        Files.readAllLines(path);
    }

    @Test
    public void testReadFileInRemovedDirectory() throws IOException {

        FileSystem fs = new TestFS().removingFiles(DIRECTORY).create();
        Path path = fs.getPath(DIRECTORY_FILE2);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        Files.readAllLines(path);
    }

    @Test
    public void testReadUnreadableFile() throws IOException {

        FileSystem fs = new TestFS().addingFile(SIMPLE_TXT, SIMPLE_TXT, Permissions._WX).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(path.toString());

        Files.readAllLines(path);
    }

    @Test
    public void testWriteToRemovedFile() throws IOException {

        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        Files.write(path, Arrays.asList("foo"));
    }

    @Test
    public void testWriteToUnwritableFile() throws IOException {

        FileSystem fs = new TestFS().alteringPermissions(SIMPLE_TXT, Permissions.R_X).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(path.toString());

        Files.write(path, Arrays.asList("foo"));
    }

    @Test
    public void testAppendToUnwritableFile() throws IOException {

        FileSystem fs = new TestFS().addingFile(SIMPLE_TXT, SIMPLE_TXT, Permissions.R__).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(path.toString());

        Files.write(path, Arrays.asList("foo"), StandardOpenOption.APPEND);
    }

    @Test
    public void testDirectoryStreamForRemovedDirectory() throws IOException {

        FileSystem fs = new TestFS().create();
        Path dirPath = fs.getPath(DIRECTORY);
        Path filePath = fs.getPath(DIRECTORY, "file1.txt");
        assertEquals(filePath.toString(), Files.newDirectoryStream(dirPath).iterator().next().toString());

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(dirPath.toString());

        FileSystem fs2 = new TestFS().removingFiles(DIRECTORY).create();
        Path dirPath2 = fs2.getPath(DIRECTORY);
        Files.newDirectoryStream(dirPath2);
    }

    @Test
    public void testDirectoryStreamForUnreadableDirectory() throws IOException {

        FileSystem fs = new TestFS().create();
        Path dirPath = fs.getPath(DIRECTORY);
        Path filePath = fs.getPath(DIRECTORY, "file1.txt");
        assertEquals(filePath.toString(), Files.newDirectoryStream(dirPath).iterator().next().toString());

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(dirPath.toString());

        FileSystem fs2 = new TestFS().addingFile(DIRECTORY, DIRECTORY, Permissions._W_).create();
        Path dirPath2 = fs2.getPath(DIRECTORY);
        Files.newDirectoryStream(dirPath2);
    }

    @Test
    public void testCreateDirectory() throws IOException {

        String tempDir = System.getProperty("java.io.tmpdir");

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(tempDir, "testfs-test-dir");
        path.toFile().deleteOnExit();

        Files.createDirectory(path);
    }

    @Test
    public void testCreateDirectoryInRemovedDirectory() throws IOException {

        FileSystem fs = new TestFS().removingFiles(DIRECTORY).create();
        Path dirPath = fs.getPath(DIRECTORY);
        Path newDirPath = dirPath.resolve("subdirectory");

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(dirPath.toString());

        Files.createDirectory(newDirPath);
    }

    @Test
    public void testCreateDirectoryInUnwritableDirectory() throws IOException {

        FileSystem fs = new TestFS().alteringPermissions(DIRECTORY, Permissions.__X).create();
        Path dirPath = fs.getPath(DIRECTORY);
        Path newDirPath = dirPath.resolve("subdirectory");

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(dirPath.toString());

        Files.createDirectory(newDirPath);
    }

    @Test
    public void testDeleteFile() throws IOException {

        Path path = Files.createTempFile("testfs-", null);
        path.toFile().deleteOnExit();

        FileSystem fs = new TestFS().create();
        Files.delete(fs.getPath(path.toString()));
    }

    @Test
    public void testDeleteRemovedFile() throws IOException {

        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        Files.delete(path);
    }

    @Test
    public void testDeleteFileInRemovedDirectory() throws IOException {

        FileSystem fs = new TestFS().removingFiles(DIRECTORY).create();
        Path dirPath = fs.getPath(DIRECTORY);
        Path filePath = fs.getPath(DIRECTORY_FILE1);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(dirPath.toString());

        Files.delete(filePath);
    }

    @Test
    public void testDeleteFileInUnwritableDirectory() throws IOException {

        FileSystem fs = new TestFS().addingFile(DIRECTORY, DIRECTORY, Permissions.___).create();
        Path dirPath = fs.getPath(DIRECTORY);
        Path filePath = fs.getPath(DIRECTORY_FILE1);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(dirPath.toString());

        Files.delete(filePath);
    }

    @Test
    public void testCopyFile() throws IOException {

        Path from = Files.createTempFile("testfs-", null);
        from.toFile().deleteOnExit();

        Path to = Files.createTempFile("testfs-", null);
        assertTrue(to.toFile().delete());

        FileSystem fs = new TestFS().create();
        Files.copy(fs.getPath(from.toString()), fs.getPath(to.toString()));
    }

    @Test
    public void testCopyRemovedFile() throws IOException {

        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path from = fs.getPath(SIMPLE_TXT);
        Path to = fs.getPath("asdfasdf.txt");

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(from.toString());

        Files.copy(from, to);
    }

    @Test
    public void testCopyUnreadableFile() throws IOException {

        FileSystem fs = new TestFS().addingFile(SIMPLE_TXT, SIMPLE_TXT, Permissions._WX).create();
        Path from = fs.getPath(SIMPLE_TXT);
        Path to = fs.getPath("asdfasdf.txt");

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(from.toString());

        Files.copy(from, to);
    }

    @Test
    public void testMoveFile() throws IOException {

        Path from = Files.createTempFile("testfs-", null);
        from.toFile().deleteOnExit();

        Path to = Files.createTempFile("testfs-", null);
        assertTrue(to.toFile().delete());

        FileSystem fs = new TestFS().create();
        Files.move(fs.getPath(from.toString()), fs.getPath(to.toString()));
    }

    @Test
    public void testMoveRemovedFile() throws IOException {

        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path from = fs.getPath(SIMPLE_TXT);
        Path to = fs.getPath("asdfasdf.txt");

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(from.toString());

        Files.move(from, to);
    }

    @Test
    public void testMoveFileFromRemovedDirectory() throws IOException {

        FileSystem fs = new TestFS().removingFiles(DIRECTORY).create();
        Path from = fs.getPath(DIRECTORY_FILE1);
        Path to = fs.getPath("asdfasdf.txt");

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(from.toString());

        Files.move(from, to);
    }

    @Test
    public void testMoveFileFromUnwritableDirectory() throws IOException {

        FileSystem fs = new TestFS().addingFile(DIRECTORY, DIRECTORY, Permissions.R_X).create();
        Path from = fs.getPath(DIRECTORY_FILE1);
        Path to = fs.getPath("asdfasdf.txt");

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(String.valueOf(from.getParent()));

        Files.move(from, to);
    }

    @Test
    public void testMoveFileToRemovedDirectory() throws IOException {

        FileSystem fs = new TestFS().removingFiles(DIRECTORY).create();
        Path from = fs.getPath(SIMPLE_TXT);
        Path to = fs.getPath(DIRECTORY_FILE1);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(String.valueOf(to.getParent()));

        Files.move(from, to);
    }

    @Test
    public void testMoveFileToUnwritableDirectory() throws IOException {

        FileSystem fs = new TestFS().addingFile(DIRECTORY, DIRECTORY, Permissions.R_X).create();
        Path from = fs.getPath(SIMPLE_TXT);
        Path to = fs.getPath(DIRECTORY_FILE1);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(String.valueOf(to.getParent()));

        Files.move(from, to);
    }

    @Test
    public void testIsHiddenForRemovedFile() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertFalse(Files.isHidden(path));

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        FileSystem fs2 = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path2 = fs2.getPath(SIMPLE_TXT);
        Files.isHidden(path2);
    }

    @Test
    public void testGetFileStoreForRemovedFile() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertNotNull(Files.getFileStore(path));

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        FileSystem fs2 = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path2 = fs2.getPath(SIMPLE_TXT);
        Files.getFileStore(path2);
    }

    @Test
    public void testExistsForRemovedFile() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertTrue(Files.exists(path));

        FileSystem fs2 = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path2 = fs2.getPath(SIMPLE_TXT);
        assertFalse(Files.exists(path2));
    }

    @Test
    public void testIsSameFile() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        Path path2 = fs.getPath(SIMPLE_TXT);
        Path path3 = fs.getPath(DIRECTORY_FILE1);

        assertTrue(Files.isSameFile(path, path2));
        assertFalse(Files.isSameFile(path, path3));
        assertFalse(Files.isSameFile(path2, path3));
    }

    @Test
    public void testGetFileAttributeViewForRemovedFile() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertNotNull(Files.getFileAttributeView(path, BasicFileAttributeView.class));

        FileSystem fs2 = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path2 = fs2.getPath(SIMPLE_TXT);
        assertNull(Files.getFileAttributeView(path2, BasicFileAttributeView.class));
    }

    @Test
    public void testReadAttributesFromRemovedFile() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertNotNull(Files.readAttributes(path, BasicFileAttributes.class));

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        FileSystem fs2 = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path2 = fs2.getPath(SIMPLE_TXT);
        Files.readAttributes(path2, BasicFileAttributes.class);
    }

    @Test
    public void testReadAttributesFromRemovedFile2() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertNotNull(Files.readAttributes(path, "*"));

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        FileSystem fs2 = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path2 = fs2.getPath(SIMPLE_TXT);
        Files.readAttributes(path2, "*");
    }

    @Test
    public void testSetAttribute() throws IOException {

        FileSystem fs = new TestFS().create();
        Path path = fs.getPath(SIMPLE_TXT);

        FileTime now = FileTime.fromMillis(System.currentTimeMillis());
        Files.setAttribute(path, "basic:lastAccessTime", now);
    }

    @Test
    public void testSetAttributeForRemovedFile() throws IOException {

        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path = fs.getPath(SIMPLE_TXT);

        expectedException.expect(NoSuchFileException.class);
        expectedException.expectMessage(path.toString());

        FileTime now = FileTime.fromMillis(System.currentTimeMillis());
        Files.setAttribute(path, "basic:lastAccessTime", now);
    }

    @Test
    public void testIsExecutableMatchesDefaultFileSystem() {

        Path defaultPath = DEFAULT_FS.getPath(SIMPLE_TXT);
        assertTrue(Files.isExecutable(defaultPath));

        Path testPath = new TestFS().create().getPath(SIMPLE_TXT);
        assertTrue(Files.isExecutable(testPath));
    }

    @Test
    public void testIsExecutableForRemovedFile() {
        FileSystem fs = new TestFS().removingFiles(SIMPLE_TXT).create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertFalse(Files.isExecutable(path));
    }

    @Test
    public void testIsExecutableForUnexecutableFile() {
        FileSystem fs = new TestFS().alteringPermissions(SIMPLE_TXT, Permissions.RW_).create();
        Path path = fs.getPath(SIMPLE_TXT);
        assertFalse(Files.isExecutable(path));
    }

    @Test
    public void testClose() throws IOException {

        String msg;
        try {
            DEFAULT_FS.close();
            fail("The default file system should not allow closing.");
            return;
        } catch (UnsupportedOperationException e) {
            msg = e.getMessage();
        }

        try {
            new TestFS().create().close();
            fail("The test file system should not allow closing, since it delegates to the default file system.");
        } catch (UnsupportedOperationException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void testIsOpen() {
        assertTrue(DEFAULT_FS.isOpen());
        assertTrue(new TestFS().create().isOpen());
    }

    @Test
    public void testIsReadOnly() {
        assertFalse(DEFAULT_FS.isReadOnly());
        assertFalse(new TestFS().create().isReadOnly());
    }

    @Test
    public void testToString() {
        String s = new TestFS().create().toString();
        assertTrue(s.startsWith("TestFileSystem[") && s.endsWith("]"));
    }

    @Test
    public void testGetSeparator() {
        String defaultSeparator = DEFAULT_FS.getSeparator();
        String testSeparator = new TestFS().create().getSeparator();
        assertEquals(defaultSeparator, testSeparator);
    }

    @Test
    public void testGetRootDirectories() {

        FileSystem fs = new TestFS().create();

        Iterator< Path > defaultRoots = DEFAULT_FS.getRootDirectories().iterator();
        Iterator< Path > testRoots = fs.getRootDirectories().iterator();

        while (defaultRoots.hasNext() && testRoots.hasNext()) {
            Path defaultRoot = defaultRoots.next();
            Path testRoot = testRoots.next();
            assertEquals(defaultRoot.toString(), testRoot.toString());
            assertEquals(DEFAULT_FS, defaultRoot.getFileSystem());
            assertEquals(fs, testRoot.getFileSystem());
        }

        assertFalse(defaultRoots.hasNext());
        assertFalse(testRoots.hasNext());
    }

    @Test
    public void testGetFileStores() {

        FileSystem fs = new TestFS().create();

        Iterator< FileStore > defaultStores = DEFAULT_FS.getFileStores().iterator();
        Iterator< FileStore > testStores = fs.getFileStores().iterator();

        while (defaultStores.hasNext() && testStores.hasNext()) {
            FileStore defaultStore = defaultStores.next();
            FileStore testStore = testStores.next();
            assertEquals(defaultStore.name(), testStore.name());
        }

        assertFalse(defaultStores.hasNext());
        assertFalse(testStores.hasNext());
    }

    @Test
    public void testSupportedFileAttributeViews() {
        Set< String > defaultViews = DEFAULT_FS.supportedFileAttributeViews();
        Set< String > testViews = new TestFS().create().supportedFileAttributeViews();
        assertEquals(defaultViews, testViews);
    }

    @Test
    public void testGetPathMatcher() {
        FileSystem fs = new TestFS().create();
        PathMatcher matcher = fs.getPathMatcher("glob:**/*.txt");
        Path path = fs.getPath(SIMPLE_TXT);
        assertTrue(matcher.matches(path));
    }

    @Test
    public void testGetUserPrincipalLookupService() {
        UserPrincipalLookupService defaultService = DEFAULT_FS.getUserPrincipalLookupService();
        UserPrincipalLookupService testService = new TestFS().create().getUserPrincipalLookupService();
        assertEquals(defaultService, testService);
    }

    @Test
    public void testNewWatchService() throws IOException {
        Class< ? > defaultService = DEFAULT_FS.newWatchService().getClass();
        Class< ? > testService = new TestFS().create().newWatchService().getClass();
        assertEquals(defaultService, testService);
    }

    @Test
    public void testProviderToString() {
        assertEquals("TestFileSystemProvider[scheme=file]", new TestFS().create().provider().toString());
    }
}
