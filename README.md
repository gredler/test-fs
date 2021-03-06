# TestFS [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.gredler/test-fs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.gredler/test-fs) [![Build Status](https://travis-ci.org/gredler/test-fs.svg?branch=master)](https://travis-ci.org/gredler/test-fs)

```java
// Give me access to the file system...
FileSystem fs = new TestFS()
    // ... but pretend that these files doesn't exist, even if they do
    .removingFiles("config/user.properties", "config/startup.properties")
    // ... and pretend that this file exists and contains my sample license
    .addingFile("user1.license", "src/test/resources/sample.license", Permissions.RWX)
    // ... and pretend this file exists, contains my sample license, but isn't readable
    .addingFile("user2.license", "src/test/resources/sample.license", Permissions._WX)
    // ... and I'm sure this file exists, but I want to pretend that it isn't writable
    .alteringPermissions("src/test/resources/input.xml", Permissions.R_X)
    // ... and please throw an IOException if I try to read the contents of this file
    .throwingExceptionOnRead("/opt/app/config.xml")
    // ... and please throw an IOException if I try to write to this file
    .throwingExceptionOnWrite("/opt/app/output.log")
    // ... k thanks!
    .create();
```

##Why?

Static state is bad. You know it, and you try to avoid it. But sometimes it's sneaky. That innocent-looking `new Date()`
call is going behind your back and talking to the system clock. Similarly, those `new File(myPath)` calls are talking to
the global file system. Java 8 introduced the [Clock](https://docs.oracle.com/javase/8/docs/api/java/time/Clock.html)
abstraction to help with the date situation, but what about your file-dependent code?

Java 7 added a new file system abstraction that can be used to eliminate this particular flavor of static state.
[FileSystemProviders](http://docs.oracle.com/javase/7/docs/api/java/nio/file/spi/FileSystemProvider.html) are associated
with a URI scheme and provide access to [FileSystems](http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html),
which themselves provide access to file [Paths](http://docs.oracle.com/javase/7/docs/api/java/nio/file/Path.html) and other
file system attributes and services. These `Path`s can then be used to read from and write to the associated file system,
using the [Files](http://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html) utility class.

If you provide your file-dependent code with a `FileSystem` instance through which to interact with the file system, you
remove this implicit global state, and open the door to the possibility of using one `FileSystem` at runtime and a different
`FileSystem` during testing. But what `FileSystem` should you use during testing?

One option is Google's [Jimfs](https://github.com/google/jimfs), an in-memory `FileSystem` implementation that allows you to
create a virtual file system according to your test needs. Jimfs is fast and powerful, but can require a significant amount
of boilerplate code to set up (and shut down) correctly. It also [doesn't support](https://github.com/google/jimfs#whats-supported)
some important features like file permissions.

TestFS is another alternative, and takes a slightly different approach: rather than providing a custom file system that needs
to be set up from scratch and then cleaned up after your tests, it's a thin wrapper around [the default file
system](http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystems.html#getDefault%28%29), with extra functionality
that allows you to selectively hide files, add simulated files, or simulate different permissions on existing files. This
alternative approach, which allows you to start with the default file system and then tweak its behavior without modifying
the actual file system, may in some cases be a better fit than the Jimfs approach of starting with a blank slate.

##Example

As an example, take the following application code:

```java
public void checkLicense() {
    File license = new File("user.license");
    if (!license.exists()) {
        throw new LicenseException(license.getPath() + "' does not exist.");
    } else if (!license.canRead()) {
        throw new LicenseException(license.getPath() + "' cannot be read.");
    } else {
        checkLicense(new FileInputStream(license));
    }
}

checkLicense();
```

Testing this code will likely involve the creation of temporary files that need to be cleaned up after your tests:

```java
@Test
public void testCheckLicenseSuccess() throws IOException {
    File src = new File("src/test/resources/sample.license");
    File dest = new File("user.license");
    FileUtils.copyFile(src, dest);
    try {
        instance.checkLicense();
    } finally {
        FileUtils.deleteQuietly(dest);
    }
}
```

However, the application code can be refactored as follows:

```java
public void checkLicense(FileSystem fs) {
    Path license = fs.getPath("user.license");
    if (!Files.exists(license)) {
        throw new LicenseException(license.getPath() + "' does not exist.");
    } else if (!Files.isReadable(license)) {
        throw new LicenseException(license.getPath() + "' cannot be read.");
    } else {
        checkLicense(Files.newInputStream(license));
    }
}

checkLicense(FileSystems.getDefault());
```

You can then use TestFS to easily simulate different test scenarios:

```java
@Test
public void testCheckLicenseSuccess() {
    FileSystem fs = new TestFS()
        .addingFile("user.license", "src/test/resources/sample.license")
        .create();
    instance.checkLicense(fs);
}

@Test(expected = LicenseException.class)
public void testCheckLicenseUnreadable() {
    FileSystem fs = new TestFS()
        .addingFile("user.license", "src/test/resources/sample.license", Permissions._WX)
        .create();
    instance.checkLicense(fs);
}

@Test(expected = LicenseException.class)
public void testCheckLicenseInexistent() {
    FileSystem fs = new TestFS()
        .removingFiles("user.license")
        .create();
    instance.checkLicense(fs);
}
```

See the JavaDoc of [the TestFS class](src/main/java/net/gredler/testfs/TestFS.java) for the full API documentation.
