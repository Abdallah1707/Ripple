package com.ripple;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test helper that finds the project's source files.
 */
public final class MainSources {

    /** The com/ripple source folder. */
    public static final Path ROOT = Path.of("src", "main", "java", "com", "ripple");

    private MainSources() {
    }

    /** All .java files in a package, without package-info. */
    public static List<Path> inPackage(String packageName) {
        return list(ROOT.resolve(packageName));
    }

    /** All main .java files, without package-info. */
    public static List<Path> all() {
        return list(ROOT);
    }

    private static List<Path> list(Path folder) {
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + folder.toAbsolutePath()
                    + ". Run the tests from the project folder (mvn test).", e);
        }
    }

    /** The package of a source file, like "view". */
    public static String packageOf(Path source) {
        return ROOT.relativize(source).getName(0).toString();
    }

    /** The class name of a source file. */
    public static String classNameOf(Path source) {
        String file = source.getFileName().toString();
        return file.substring(0, file.length() - ".java".length());
    }
}
