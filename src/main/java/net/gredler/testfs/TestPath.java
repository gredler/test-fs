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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**
 * {@link Path} wrapper.
 */
final class TestPath implements Path {

    /** The wrapped path. */
    private final Path p;

    /** The test file system that created this path wrapper. */
    private final TestFileSystem fs;

    /**
     * Creates a new instance.
     *
     * @param p the wrapped path
     * @param fs the test file system that created this path wrapper
     */
    private TestPath(Path p, TestFileSystem fs) {
        this.p = p;
        this.fs = fs;
    }

    /**
     * Wraps the specified path.
     *
     * @param p the path to wrap
     * @param fs the test file system creating the path wrapper
     * @return the path wrapper created, or the original path passed in if it's already wrapped
     */
    static TestPath wrap(Path p, TestFileSystem fs) {
        if (p instanceof TestPath) {
            // if already wrapped, don't wrap a wrapper
            return (TestPath) p;
        } else {
            // wrap it
            return new TestPath(p, fs);
        }
    }

    /**
     * Returns the wrapped path.
     *
     * @return the wrapped path
     */
    Path unwrap() {
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAbsolute() {
        return p.isAbsolute();
    }

    /** {@inheritDoc} */
    @Override
    public Path getRoot() {
        Path root = p.getRoot();
        return (root != null ? new TestPath(root, fs) : null);
    }

    /** {@inheritDoc} */
    @Override
    public Path getFileName() {
        return new TestPath(p.getFileName(), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path getParent() {
        Path parent = p.getParent();
        return (parent != null ? new TestPath(parent, fs) : null);
    }

    /** {@inheritDoc} */
    @Override
    public int getNameCount() {
        return p.getNameCount();
    }

    /** {@inheritDoc} */
    @Override
    public Path getName(int index) {
        return new TestPath(p.getName(index), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return new TestPath(p.subpath(beginIndex, endIndex), fs);
    }

    /** {@inheritDoc} */
    @Override
    public boolean startsWith(Path other) {
        Path unwrapped = ((TestPath) other).unwrap();
        return p.startsWith(unwrapped);
    }

    /** {@inheritDoc} */
    @Override
    public boolean startsWith(String other) {
        return p.startsWith(other);
    }

    /** {@inheritDoc} */
    @Override
    public boolean endsWith(Path other) {
        Path unwrapped = ((TestPath) other).unwrap();
        return p.endsWith(unwrapped);
    }

    /** {@inheritDoc} */
    @Override
    public boolean endsWith(String other) {
        return p.endsWith(other);
    }

    /** {@inheritDoc} */
    @Override
    public Path normalize() {
        return new TestPath(p.normalize(), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path resolve(Path other) {
        Path unwrapped = ((TestPath) other).unwrap();
        return new TestPath(p.resolve(unwrapped), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path resolve(String other) {
        return new TestPath(p.resolve(other), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path resolveSibling(Path other) {
        Path unwrapped = ((TestPath) other).unwrap();
        return new TestPath(p.resolveSibling(unwrapped), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path resolveSibling(String other) {
        return new TestPath(p.resolveSibling(other), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path relativize(Path other) {
        Path unwrapped = ((TestPath) other).unwrap();
        return new TestPath(p.relativize(unwrapped), fs);
    }

    /** {@inheritDoc} */
    @Override
    public URI toUri() {
        return p.toUri();
    }

    /** {@inheritDoc} */
    @Override
    public Path toAbsolutePath() {
        return new TestPath(p.toAbsolutePath(), fs);
    }

    /** {@inheritDoc} */
    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return new TestPath(p.toRealPath(options), fs);
    }

    /** {@inheritDoc} */
    @Override
    public File toFile() {
        return p.toFile();
    }

    /** {@inheritDoc} */
    @Override
    public WatchKey register(WatchService watcher, Kind< ? >[] events, Modifier... modifiers) throws IOException {
        return p.register(watcher, events, modifiers);
    }

    /** {@inheritDoc} */
    @Override
    public WatchKey register(WatchService watcher, Kind< ? >... events) throws IOException {
        return p.register(watcher, events);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator< Path > iterator() {

        final Iterator< Path > i = p.iterator();

        return new Iterator< Path >() {
            @Override
            public Path next() {
                return new TestPath(i.next(), fs);
            }
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Path other) {
        Path unwrapped = ((TestPath) other).unwrap();
        return p.compareTo(unwrapped);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TestPath) {
            Path unwrapped = ((TestPath) other).unwrap();
            return p.equals(unwrapped);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return p.hashCode() + 1; // consistent with native paths, but different
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return p.toString();
    }
}
