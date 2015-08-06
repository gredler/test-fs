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
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Set;

/**
 * {@link FileSystem} wrapper.
 */
final class TestFileSystem extends FileSystem {

    /** The wrapped file system. */
    private final FileSystem fs;

    /** The wrapped file system's wrapped provider. */
    private final TestFileSystemProvider provider; // NOPMD

    /**
     * Creates a new instance.
     *
     * @param fs the wrapped file system
     * @param provider the wrapped file system's wrapped provider
     */
    TestFileSystem(FileSystem fs, TestFileSystemProvider provider) {
        this.fs = fs;
        this.provider = provider;
    }

    /** {@inheritDoc} */
    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        fs.close();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOpen() {
        return fs.isOpen();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadOnly() {
        return fs.isReadOnly();
    }

    /** {@inheritDoc} */
    @Override
    public String getSeparator() {
        return fs.getSeparator();
    }

    /** {@inheritDoc} */
    @Override
    public Iterable< Path > getRootDirectories() {

        final Iterator< Path > i = fs.getRootDirectories().iterator();

        return new Iterable< Path >() {
            @Override
            public Iterator< Path > iterator() {
                return new Iterator< Path >() {
                    @Override
                    public Path next() {
                        return TestPath.wrap(i.next(), TestFileSystem.this);
                    }
                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Iterable< FileStore > getFileStores() {
        return fs.getFileStores();
    }

    /** {@inheritDoc} */
    @Override
    public Set< String > supportedFileAttributeViews() {
        return fs.supportedFileAttributeViews();
    }

    /** {@inheritDoc} */
    @Override
    public Path getPath(String first, String... more) {
        return TestPath.wrap(fs.getPath(first, more), this);
    }

    /** {@inheritDoc} */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return fs.getPathMatcher(syntaxAndPattern);
    }

    /** {@inheritDoc} */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return fs.getUserPrincipalLookupService();
    }

    /** {@inheritDoc} */
    @Override
    public WatchService newWatchService() throws IOException {
        return fs.newWatchService();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestFileSystem) {
            TestFileSystem other = (TestFileSystem) obj;
            return other.fs.equals(this.fs) && other.provider.equals(this.provider);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fs.hashCode();
        result = prime * result + provider.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('[');
        for (Iterator< Path > i = fs.getRootDirectories().iterator(); i.hasNext();) {
            sb.append(i.next().toString());
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
