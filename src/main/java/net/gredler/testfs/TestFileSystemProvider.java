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

import static net.gredler.testfs.Permissions.RWX;
import static net.gredler.testfs.Permissions.RW_;
import static net.gredler.testfs.Permissions.R_X;
import static net.gredler.testfs.Permissions.R__;
import static net.gredler.testfs.Permissions._WX;
import static net.gredler.testfs.Permissions._W_;
import static net.gredler.testfs.Permissions.__X;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link FileSystemProvider} wrapper.
 */
final class TestFileSystemProvider extends FileSystemProvider {

    /** The wrapped file system provider. */
    private final FileSystemProvider provider;

    /** Paths that don't exist in this file system provider. */
    private final List< Path > removedPaths;

    /** Targets for additional paths that only exist in this file system provider. */
    private final Map< Path, Path > addedPathTargets;

    /** Permissions for additional paths that only exist in this file system provider. */
    private final Map< Path, Permissions > addedPathPermissions;

    /** Access to these files using these access modes should trigger an {@link IOException}. */
    private final Map< Path, Set< AccessMode > > exceptionPaths;

    /**
     * Creates a new instance.
     *
     * @param fs the wrapped file system
     * @param removedPaths paths that don't exist in this file system provider
     * @param addedPathTargets targets for additional paths that only exist in this file system provider
     * @param addedPathPermissions permissions for additional paths that only exist in this file system provider
     * @param exceptionPaths access to these files using these access modes should trigger an {@link IOException}
     */
    TestFileSystemProvider(FileSystem fs, List< String > removedPaths, Map< String, String > addedPathTargets,
                    Map< String, Permissions > addedPathPermissions, Map< String, Set< AccessMode > > exceptionPaths) {

        this.provider = fs.provider();

        this.removedPaths = new ArrayList< Path >();
        for (String removedPath : removedPaths) {
            this.removedPaths.add(fs.getPath(removedPath).toAbsolutePath());
        }

        this.addedPathTargets = new HashMap<>();
        for (Map.Entry< String, String > addedPath : addedPathTargets.entrySet()) {
            this.addedPathTargets.put(fs.getPath(addedPath.getKey()).toAbsolutePath(), fs.getPath(addedPath.getValue())
                            .toAbsolutePath());
        }

        this.addedPathPermissions = new HashMap<>();
        for (Map.Entry< String, Permissions > addedPathPermission : addedPathPermissions.entrySet()) {
            this.addedPathPermissions.put(fs.getPath(addedPathPermission.getKey()).toAbsolutePath(),
                            addedPathPermission.getValue());
        }

        this.exceptionPaths = new HashMap<>();
        for (Map.Entry< String, Set< AccessMode > > entry : exceptionPaths.entrySet()) {
            this.exceptionPaths.put(fs.getPath(entry.getKey()).toAbsolutePath(), entry.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getScheme() {
        return provider.getScheme();
    }

    /** {@inheritDoc} */
    @Override
    public FileSystem newFileSystem(URI uri, Map< String, ? > env) throws IOException {
        return new TestFileSystem(provider.newFileSystem(uri, env), this);
    }

    /** {@inheritDoc} */
    @Override
    public FileSystem getFileSystem(URI uri) {
        return new TestFileSystem(provider.getFileSystem(uri), this);
    }

    /** {@inheritDoc} */
    @Override
    public Path getPath(URI uri) {
        return TestPath.wrap(provider.getPath(uri), new TestFileSystem(provider.getFileSystem(uri), this));
    }

    /** {@inheritDoc} */
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set< ? extends OpenOption > options,
                    FileAttribute< ? >... attrs) throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);
        checkPermissions(path, true, toAccessModes(options));

        Path target = addedPathTargets.get(path.toAbsolutePath());
        if (target != null) {
            return provider.newByteChannel(target, options, attrs);
        }

        return provider.newByteChannel(path, options, attrs);
    }

    private static AccessMode[] toAccessModes(Set< ? extends OpenOption > options) {

        boolean write = options.contains(StandardOpenOption.WRITE);
        boolean append = options.contains(StandardOpenOption.APPEND);

        List< AccessMode > modes = new ArrayList<>();
        if (write || append) {
            modes.add(AccessMode.WRITE);
        } else {
            modes.add(AccessMode.READ);
        }

        return modes.toArray(new AccessMode[modes.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public DirectoryStream< Path > newDirectoryStream(Path dir, Filter< ? super Path > filter) throws IOException {

        dir = ((TestPath) dir).unwrap();

        checkIfRemoved(dir);
        checkPermissions(dir, true, AccessMode.READ);

        return provider.newDirectoryStream(dir, filter);
    }

    /** {@inheritDoc} */
    @Override
    public void createDirectory(Path dir, FileAttribute< ? >... attrs) throws IOException {

        dir = ((TestPath) dir).unwrap();

        Path parent = dir.getParent();
        if (parent != null) {
            checkIfRemoved(parent);
            checkPermissions(parent, true, AccessMode.WRITE);
        }

        provider.createDirectory(dir, attrs);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Path path) throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        Path parent = path.getParent();
        if (parent != null) {
            checkPermissions(parent, true, AccessMode.WRITE);
        }

        provider.delete(path);
    }

    /** {@inheritDoc} */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

        source = ((TestPath) source).unwrap();
        target = ((TestPath) target).unwrap();

        checkIfRemoved(source);
        checkPermissions(source, true, AccessMode.READ);

        provider.copy(source, target, options);
    }

    /** {@inheritDoc} */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

        source = ((TestPath) source).unwrap();
        target = ((TestPath) target).unwrap();

        checkIfRemoved(source);

        Path sourceParent = source.getParent();
        if (sourceParent != null) {
            checkIfRemoved(sourceParent);
            checkPermissions(sourceParent, true, AccessMode.WRITE);
        }

        Path targetParent = target.getParent();
        if (targetParent != null) {
            checkIfRemoved(targetParent);
            checkPermissions(targetParent, true, AccessMode.WRITE);
        }

        provider.move(source, target, options);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {

        path = ((TestPath) path).unwrap();
        path2 = ((TestPath) path2).unwrap();

        return provider.isSameFile(path, path2);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHidden(Path path) throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        return provider.isHidden(path);
    }

    /** {@inheritDoc} */
    @Override
    public FileStore getFileStore(Path path) throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        return provider.getFileStore(path);
    }

    /** {@inheritDoc} */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        boolean checked = checkPermissions(path, false, modes);
        if (!checked) {
            provider.checkAccess(path, modes);
        }
    }

    /** {@inheritDoc} */
    @Override
    public < V extends FileAttributeView > V getFileAttributeView(Path path, Class< V > type, LinkOption... options) {

        path = ((TestPath) path).unwrap();

        try {
            checkIfRemoved(path);
        } catch (NoSuchFileException e) {
            return null;
        }

        return provider.getFileAttributeView(path, type, options);
    }

    /** {@inheritDoc} */
    @Override
    public < A extends BasicFileAttributes > A readAttributes(Path path, Class< A > type, LinkOption... options)
                    throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        return provider.readAttributes(path, type, options);
    }

    /** {@inheritDoc} */
    @Override
    public Map< String, Object > readAttributes(Path path, String attributes, LinkOption... options)
                    throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        return provider.readAttributes(path, attributes, options);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

        path = ((TestPath) path).unwrap();

        checkIfRemoved(path);

        provider.setAttribute(path, attribute, value, options);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestFileSystemProvider) {
            TestFileSystemProvider other = (TestFileSystemProvider) obj;
            return other.provider.equals(this.provider)
                            && other.removedPaths.equals(this.removedPaths)
                            && other.addedPathTargets.equals(this.addedPathTargets)
                            && other.addedPathPermissions.equals(this.addedPathPermissions);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + provider.hashCode();
        result = prime * result + removedPaths.hashCode();
        result = prime * result + addedPathTargets.hashCode();
        result = prime * result + addedPathPermissions.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[scheme=" + getScheme() + "]";
    }

    private void checkIfRemoved(Path path) throws NoSuchFileException {
        String originalPath = path.toString();
        for (; path != null; path = path.getParent()) {
            if (removedPaths.contains(path.toAbsolutePath())) {
                throw new NoSuchFileException(originalPath);
            }
        }
    }

    private boolean checkPermissions(Path path, boolean checkExceptions, AccessMode... modes)
                    throws AccessDeniedException, IOException {
        Permissions permissions = addedPathPermissions.get(path.toAbsolutePath());
        boolean hasPermissions = permissions != null;
        if (hasPermissions) {
            for (AccessMode mode : modes) {
                switch (mode) {
                    case READ:
                        if (permissions != RWX && permissions != R_X && permissions != RW_ && permissions != R__) {
                            throw new AccessDeniedException(path.toString());
                        }
                        break;
                    case WRITE:
                        if (permissions != RWX && permissions != _WX && permissions != RW_ && permissions != _W_) {
                            throw new AccessDeniedException(path.toString());
                        }
                        break;
                    case EXECUTE:
                        if (permissions != RWX && permissions != _WX && permissions != R_X && permissions != __X) {
                            throw new AccessDeniedException(path.toString());
                        }
                        break;
                    default:
                        // impossible
                }
            }
        }
        if (checkExceptions) {
            checkExceptions(path, modes);
        }
        return hasPermissions;
    }

    private void checkExceptions(Path path, AccessMode... modes) throws IOException {
        Set< AccessMode > errorModes = exceptionPaths.get(path.toAbsolutePath());
        if (errorModes != null) {
            for (AccessMode mode : modes) {
                if (errorModes.contains(mode)) {
                    throw new IOException(path.toString());
                }
            }
        }
    }
}
