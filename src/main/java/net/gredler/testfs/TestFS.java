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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Test file system builder. A test file system simply wraps the default file system, allowing the user to simulate the existence
 * or non-existence of certain individual files.
 *
 * <p>
 * For example, the following test file system behaves exactly like the default file system, with all reads and writes passing
 * through to the default file system:
 *
 * <pre>{@code
 * FileSystem fs = new TestFS().create();
 * }</pre>
 *
 * <p>
 * The following test file system also behaves exactly like the default file system, except that the files <code>/test/file1.log</code>
 * and <code>/test/file2.log</code> don't appear to exist, even if in reality they do exist:
 *
 * <pre>{@code
 * FileSystem fs = new TestFS().removingFiles("/test/file1.log", "/test/file2.log").create();
 * }</pre>
 *
 * <p>
 * The following test file system behaves exactly like the default file system, except that the files <code>/test/file1.log</code> and
 * <code>/test/file2.log</code> exist but aren't writable, regardless of whether or not they exist in reality, and regardless of whether
 * or not they are writable in reality. If either simulated file is read from, the read will be delegated to the file at
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

    /** Paths to files that should not exist in the test file system, regardless of whether or not they actually exist. */
    private final List< String > removedPaths;

    /** Target paths for files that should exist in the test file system, regardless of whether or not they actually exist. */
    private final Map< String, String > addedPathTargets;

    /** Permissions for files that should exist in the test file system, regardless of whether or not they actually exist. */
    private final Map< String, Permissions > addedPathPermissions;

    /**
     * Creates a new test file system builder.
     */
    public TestFS() {
        removedPaths = new ArrayList<>();
        addedPathTargets = new HashMap<>();
        addedPathPermissions = new HashMap<>();
    }

    /**
     * Simulates the existence of the specified file path, regardless of whether or not it actually exists.
     *
     * @param path the path of the file that should exist in the context of the test file system
     * @param targetPath path to an existing file to which reads and other file system operations will be delegated (this file must exist)
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
     * @param targetPath path to an existing file to which reads and other file system operations will be delegated (this file must exist)
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
     * @param path the path of the file whose permissions are to be simulated (this file must exist)
     * @param permissions the simulated permissions to use for the specified file
     * @return this test file system builder, ready for further customization
     * @throws IllegalArgumentException if the specified file does not exist
     */
    public TestFS alteringPermissions(String path, Permissions permissions) {
        addingFile(path, path, permissions);
        return this;
    }

    /**
     * Creates and returns the test file system.
     *
     * @return the test file system
     */
    public FileSystem create() {
        FileSystem fs = FileSystems.getDefault();
        TestFileSystemProvider provider = new TestFileSystemProvider(fs, removedPaths, addedPathTargets, addedPathPermissions);
        return new TestFileSystem(fs, provider);
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
