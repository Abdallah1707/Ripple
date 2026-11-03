package com.ripple;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the project's ground rules by reading the source files (Phase 1, Section 6), so the
 * MVC separation cannot be broken by accident later:
 * <ul>
 *   <li>a view never imports {@code java.sql} or a repository (this replaces the manual
 *       {@code grep} from {@code PHASE1.md});</li>
 *   <li>only views (and {@code Main}, which builds the window) use Swing or AWT;</li>
 *   <li>only {@code Main} and the views refer to the view package;</li>
 *   <li>every class has a Javadoc comment.</li>
 * </ul>
 */
class ArchitectureRulesTest {

    /** Packages that are allowed to use Swing / AWT. */
    private static final Set<String> SWING_ALLOWED = Set.of("view", "app");
    /** Packages that are allowed to refer to {@code com.ripple.view}. */
    private static final Set<String> VIEW_ALLOWED = Set.of("view", "app");

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?m)^(public\\s+|protected\\s+|private\\s+)?(static\\s+)?(final\\s+|abstract\\s+|sealed\\s+)*"
                    + "(class|interface|enum|record)\\s+\\w+");

    @Test
    void viewsNeverUseTheDatabaseOrRepositories() throws IOException {
        List<String> offenders = filesImporting(MainSources.inPackage("view"),
                "java\\.sql|com\\.ripple\\.repository");

        assertTrue(offenders.isEmpty(), "A view must call a controller instead of using the database: " + offenders);
    }

    @Test
    void onlyViewsAndMainUseSwing() throws IOException {
        List<Path> others = MainSources.all().stream()
                .filter(source -> !SWING_ALLOWED.contains(MainSources.packageOf(source))).toList();

        List<String> offenders = filesImporting(others, "javax\\.swing|java\\.awt");

        assertTrue(offenders.isEmpty(), "Model, controller and pattern classes must not know about Swing: "
                + offenders);
    }

    @Test
    void onlyViewsAndMainReferToTheViewPackage() throws IOException {
        List<Path> others = MainSources.all().stream()
                .filter(source -> !VIEW_ALLOWED.contains(MainSources.packageOf(source))).toList();

        List<String> offenders = filesImporting(others, "com\\.ripple\\.view");

        assertTrue(offenders.isEmpty(), "Dependencies must point from view to controller to model, "
                + "never back: " + offenders);
    }

    @Test
    void everyClassHasAJavadocComment() throws IOException {
        List<String> without = new ArrayList<>();
        for (Path source : MainSources.all()) {
            if (!hasJavadocOnTopLevelType(Files.readString(source, StandardCharsets.UTF_8))) {
                without.add(MainSources.classNameOf(source));
            }
        }

        assertTrue(without.isEmpty(), "Add a short Javadoc comment on top of: " + without);
    }

    /** The files (as class names) that have an {@code import} line matching the given regex. */
    private static List<String> filesImporting(List<Path> sources, String importRegex) throws IOException {
        Pattern importLine = Pattern.compile("(?m)^import\\s+(static\\s+)?(" + importRegex + ")");
        List<String> offenders = new ArrayList<>();
        for (Path source : sources) {
            if (importLine.matcher(Files.readString(source, StandardCharsets.UTF_8)).find()) {
                offenders.add(MainSources.classNameOf(source));
            }
        }
        return offenders;
    }

    /** True if the comment right above the class / interface / enum / record declaration is a Javadoc. */
    private static boolean hasJavadocOnTopLevelType(String source) {
        Matcher declaration = TYPE_DECLARATION.matcher(source);
        if (!declaration.find()) {
            return false;
        }
        List<String> above = new ArrayList<>(List.of(source.substring(0, declaration.start()).split("\\R")));
        // Annotations (e.g. @FunctionalInterface) may sit between the comment and the declaration.
        while (!above.isEmpty() && (above.get(above.size() - 1).isBlank()
                || above.get(above.size() - 1).strip().startsWith("@"))) {
            above.remove(above.size() - 1);
        }
        String text = String.join("\n", above).strip();
        // The comment that ends right above the declaration must itself start with "/**".
        return text.endsWith("*/") && text.startsWith("/**", text.lastIndexOf("/*"));
    }
}
