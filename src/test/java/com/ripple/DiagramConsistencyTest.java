package com.ripple;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails the build if {@code docs/class-diagram.md} and the code drift apart (Phase 1, Section 6).
 *
 * <p>The "complete diagram" in section 2 of the document is the team contract, so:
 * <ul>
 *   <li>every class drawn in it must exist in the code;</li>
 *   <li>every class in the code must be drawn in it;</li>
 *   <li>each class must sit in the diagram namespace that matches its Java package;</li>
 *   <li>the small per-pattern diagrams may only use classes of the complete diagram.</li>
 * </ul>
 * It compares class names and packages, not every field and method.
 */
class DiagramConsistencyTest {

    private static final Path DIAGRAM = Path.of("docs", "class-diagram.md");
    private static final Pattern CLASS_LINE = Pattern.compile("(?m)^\\s*class\\s+(\\w+)");
    private static final Pattern NAMESPACE_BLOCK = Pattern.compile("(?ms)^namespace\\s+(\\w+)\\s*\\{(.*?)^\\}");
    private static final Pattern MERMAID_BLOCK = Pattern.compile("(?s)```mermaid\\R(.*?)```");

    @Test
    void everyClassInTheDiagramExistsInTheCode() throws IOException {
        Set<String> missing = new TreeSet<>(diagramClasses().keySet());
        missing.removeAll(codeClasses().keySet());

        assertTrue(missing.isEmpty(), "Drawn in docs/class-diagram.md but not found in src/main/java: " + missing
                + ". Create the class, or remove it from the diagram.");
    }

    @Test
    void everyClassInTheCodeIsInTheDiagram() throws IOException {
        Set<String> undrawn = new TreeSet<>(codeClasses().keySet());
        undrawn.removeAll(diagramClasses().keySet());

        assertTrue(undrawn.isEmpty(), "In src/main/java but missing from the complete diagram in "
                + "docs/class-diagram.md: " + undrawn + ". Add it to the diagram (and to the class tables).");
    }

    @Test
    void eachClassIsDrawnInTheNamespaceOfItsPackage() throws IOException {
        Map<String, String> inDiagram = diagramClasses();
        List<String> wrong = new ArrayList<>();
        codeClasses().forEach((name, packageName) -> {
            String namespace = inDiagram.get(name);
            if (namespace != null && !namespace.equals(packageName)) {
                wrong.add(name + " is in package '" + packageName + "' but drawn in namespace '" + namespace + "'");
            }
        });

        assertTrue(wrong.isEmpty(), "Namespace and package disagree: " + wrong);
    }

    @Test
    void patternDiagramsOnlyUseClassesOfTheCompleteDiagram() throws IOException {
        Set<String> known = diagramClasses().keySet();
        Set<String> unknown = new TreeSet<>();
        Matcher block = MERMAID_BLOCK.matcher(read());
        while (block.find()) {
            Matcher cls = CLASS_LINE.matcher(block.group(1));
            while (cls.find()) {
                if (!known.contains(cls.group(1))) {
                    unknown.add(cls.group(1));
                }
            }
        }

        assertTrue(unknown.isEmpty(), "Used in a pattern diagram but not in the complete diagram: " + unknown);
    }

    /** Class name -> namespace, for every class in section 2 ("Complete diagram") of the document. */
    private static Map<String, String> diagramClasses() throws IOException {
        String markdown = read();
        int start = markdown.indexOf("## 2. Complete diagram");
        int end = markdown.indexOf("## 3.", start);
        assertTrue(start >= 0 && end > start,
                "docs/class-diagram.md must contain the headings '## 2. Complete diagram' and '## 3.'");

        Map<String, String> classes = new TreeMap<>();
        Matcher namespace = NAMESPACE_BLOCK.matcher(markdown.substring(start, end));
        while (namespace.find()) {
            Matcher cls = CLASS_LINE.matcher(namespace.group(2));
            while (cls.find()) {
                classes.put(cls.group(1), namespace.group(1));
            }
        }
        return classes;
    }

    /** Class name -> package folder, for every Java file in {@code src/main/java}. */
    private static Map<String, String> codeClasses() {
        Map<String, String> classes = new TreeMap<>();
        for (Path source : MainSources.all()) {
            classes.put(MainSources.classNameOf(source), MainSources.packageOf(source));
        }
        return classes;
    }

    private static String read() throws IOException {
        return Files.readString(DIAGRAM, StandardCharsets.UTF_8);
    }
}
