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

import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Test file system builder. A test file system simply wraps the default file system, allowing the user to
 * simulate the existence or non-existence of certain individual files.
 *
 * <p>
 * For example, the following test file system behaves exactly like the default file system, with all reads
 * and writes passing through to the default file system:
 *
 * <pre>{@code
 * FileSystem fs = new TestFS().create();
 * }</pre>
 *
 * <p>
 * The following test file system also behaves exactly like the default file system, except that the files
 * <code>/test/file1.log</code> and <code>/test/file2.log</code> don't appear to exist, even if in reality
 * they do exist:
 *
 * <pre>{@code
 * FileSystem fs = new TestFS()
 *     .removingFiles("/test/file1.log", "/test/file2.log")
 *     .create();
 * }</pre>
 *
 * <p>
 * The following test file system behaves exactly like the default file system, but simulates a read-only
 * temporary directory:
 *
 * <pre>{@code
 * String tempDir = System.getProperty("java.io.tmpdir");
 * FileSystem fs = new TestFS()
 *     .alteringPermissions(tempDir, Permissions.R_X)
 *     .create();
 * }</pre>
 *
 * <p>
 * The following test file system behaves exactly like the default file system, except that the files
 * <code>/test/file1.log</code> and <code>/test/file2.log</code> exist but aren't writable, regardless of
 * whether or not they exist in reality, and regardless of whether or not they are writable in reality.
 * If either simulated file is read from, the read will be delegated to the file at
 * <code>src/test/resources/my.log</code>.
 *
 * <pre>{@code
 * FileSystem fs = new TestFS()
 *     .addingFile("/test/file1.log", "src/test/resources/my.log", Permissions.R_X)
 *     .addingFile("/test/file2.log", "src/test/resources/my.log", Permissions.R_X)
 *     .create();
 * }</pre>
 */
public final class TestFS {

    /**
     * Paths to files that should not exist in the test file system,
     * regardless of whether or not they actually exist.
     */
    private final List< String > removedPaths;

    /**
     * Target paths for files that should exist in the test file system,
     * regardless of whether or not they actually exist.
     */
    private final Map< String, String > addedPathTargets;

    /**
     * Permissions for files that should exist in the test file system,
     * regardless of whether or not they actually exist.
     */
    private final Map< String, Permissions > addedPathPermissions;

    /**
     * Access to these files using these access modes should trigger
     * an {@link IOException}.
     */
    private final Map< String, Set< AccessMode > > exceptionPaths;

    /**
     * Creates a new test file system builder.
     */
    public TestFS() {
        removedPaths = new ArrayList<>();
        addedPathTargets = new HashMap<>();
        addedPathPermissions = new HashMap<>();
        exceptionPaths = new HashMap<>();
    }

    /**
     * Simulates the existence of the specified file path, regardless of whether or not it actually exists.
     *
     * @param path the path of the file that should exist in the context of the test file system
     * @param targetPath path to an existing file to which reads and other file system operations will be delegated
     * @return this test file system builder, ready for further customization
     * @throws IllegalArgumentException if the specified target file does not exist
     */
    public TestFS addingFile(String path, String targetPath) {
        return addingFile(path, targetPath, Permissions.RWX);
    }

    /**
     * Simulates the existence of the specified file path, regardless of whether or not it actually exists.
     *
     * @param path the path of the file that should exist in the context of the test file system
     * @param targetPath path to an existing file to which reads and other file system operations will be delegated
     * @param permissions custom read/write/execute permissions to apply to the simulated file
     * @return this test file system builder, ready for further customization
     * @throws IllegalArgumentException if the specified target file does not exist
     */
    public TestFS addingFile(String path, String targetPath, Permissions permissions) {

        Path target = FileSystems.getDefault().getPath(targetPath);
        if (!Files.exists(target)) {
            throw new IllegalArgumentException(targetPath + " must exist, but does not");
        }

        addedPathTargets.put(path, targetPath);
        addedPathPermissions.put(path, permissions);
        return this;
    }

    /**
     * Simulates the non-existence of the specified file paths, regardless of whether or not they actually exist.
     *
     * @param paths the paths of the files that should not exist in the context of the test file system
     * @return this test file system builder, ready for further customization
     */
    public TestFS removingFiles(String... paths) {
        removedPaths.addAll(Arrays.asList(paths));
        return this;
    }

    /**
     * Simulates the specified permissions on the specified file.
     *
     * @param path the path of the file whose permissions are to be simulated
     * @param permissions the simulated permissions to use for the specified file
     * @return this test file system builder, ready for further customization
     * @throws IllegalArgumentException if the specified file does not exist
     */
    public TestFS alteringPermissions(String path, Permissions permissions) {
        addingFile(path, path, permissions);
        return this;
    }

    /**
     * Simulates an {@link IOException} whenever the contents of the specified files are read.
     *
     * @param paths the paths of the files which are to trigger an {@link IOException} on read
     * @return this test file system builder, ready for further customization
     */
    public TestFS throwingExceptionOnRead(String... paths) {
        return throwingException(paths, AccessMode.READ);
    }

    /**
     * Simulates an {@link IOException} whenever the specified files are written to.
     *
     * @param paths the paths of the files which are to trigger an {@link IOException} on write
     * @return this test file system builder, ready for further customization
     */
    public TestFS throwingExceptionOnWrite(String... paths) {
        return throwingException(paths, AccessMode.WRITE);
    }

    /**
     * Simulates an {@link IOException} whenever the specified files are read from or written to.
     *
     * @param paths the paths of the files which are to trigger an {@link IOException} on read or write
     * @param mode the access mode that should trigger the {@link IOException} (read or write)
     * @return this test file system builder, ready for further customization
     */
    private TestFS throwingException(String[] paths, AccessMode mode) {
        for (String path : paths) {
            Set< AccessMode > modes = exceptionPaths.get(path);
            if (modes == null) {
                modes = new HashSet<>(3);
                exceptionPaths.put(path, modes);
            }
            modes.add(mode);
        }
        return this;
    }

    /**
     * Creates and returns the test file system.
     *
     * @return the test file system
     */
    public FileSystem create() {
        FileSystem fs = FileSystems.getDefault();
        TestFileSystemProvider p = new TestFileSystemProvider(fs, removedPaths,
                        addedPathTargets, addedPathPermissions, exceptionPaths);
        return new TestFileSystem(fs, p);
    }

    /**
     * Alias for {@link #create()}; particularly useful to enthusiastic Southerners.
     *
     * @return the test file system
     */
    public FileSystem gitErDone() {
        return create();
    }
}
