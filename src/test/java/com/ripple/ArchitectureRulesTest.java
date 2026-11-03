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
 * Checks the project rules by reading the source files:
 * views don't use SQL or repositories, only views and Main use Swing,
 * only views and Main use the view package, and every class has a Javadoc.
 */
class ArchitectureRulesTest {

    /** Packages allowed to use Swing. */
    private static final Set<String> SWING_ALLOWED = Set.of("view", "app");
    /** Packages allowed to use the view package. */
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

    /** Files with an import matching the regex. */
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

    /** True if the class has a Javadoc right above it. */
    private static boolean hasJavadocOnTopLevelType(String source) {
        Matcher declaration = TYPE_DECLARATION.matcher(source);
        if (!declaration.find()) {
            return false;
        }
        List<String> above = new ArrayList<>(List.of(source.substring(0, declaration.start()).split("\\R")));
        // skip annotations between the comment and the class
        while (!above.isEmpty() && (above.get(above.size() - 1).isBlank()
                || above.get(above.size() - 1).strip().startsWith("@"))) {
            above.remove(above.size() - 1);
        }
        String text = String.join("\n", above).strip();
        // the comment must start with /**
        return text.endsWith("*/") && text.startsWith("/**", text.lastIndexOf("/*"));
    }
}
