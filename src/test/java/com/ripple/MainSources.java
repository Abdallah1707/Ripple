package com.ripple;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test helper: finds Ripple's own source files so tests can check the project's structure.
 *
 * <p>Maven runs tests from the project folder, so the paths below are relative to it.
 */
public final class MainSources {

    /** The folder that holds {@code com/ripple/...}. */
    public static final Path ROOT = Path.of("src", "main", "java", "com", "ripple");

    private MainSources() {
    }

    /**
     * Lists every {@code .java} file under one package folder of {@code com.ripple}.
     *
     * @param packageName the sub-package, e.g. {@code "view"}
     * @return its source files, without {@code package-info.java}
     */
    public static List<Path> inPackage(String packageName) {
        return list(ROOT.resolve(packageName));
    }

    /**
     * Lists every {@code .java} file of Ripple, without {@code package-info.java}.
     *
     * @return all main source files
     */
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

    /**
     * The package a source file belongs to, taken from its folder, e.g. {@code "view"} for
     * {@code src/main/java/com/ripple/view/MainFrame.java}.
     *
     * @param source a file returned by {@link #all()}
     * @return the name of the folder directly under {@code com/ripple}
     */
    public static String packageOf(Path source) {
        return ROOT.relativize(source).getName(0).toString();
    }

    /**
     * The class name of a source file, e.g. {@code "MainFrame"}.
     *
     * @param source a file returned by {@link #all()}
     * @return the file name without {@code .java}
     */
    public static String classNameOf(Path source) {
        String file = source.getFileName().toString();
        return file.substring(0, file.length() - ".java".length());
    }
}
