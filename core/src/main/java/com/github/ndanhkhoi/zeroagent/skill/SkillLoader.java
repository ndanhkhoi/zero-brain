package com.github.ndanhkhoi.zeroagent.skill;

import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterNode;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility responsible for scanning local directories and dynamically loading
 * {@link Skill} configurations at runtime using flexmark-java for robust parsing.
 */
public class SkillLoader {
    private final List<Skill> skills = new ArrayList<>();
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    private SkillLoader(Parser parser, HtmlRenderer htmlRenderer) {
        this.parser = parser;
        this.htmlRenderer = htmlRenderer;
    }

    /**
     * Creates a loader with flexmark-java configured for YAML frontmatter parsing.
     *
     * @return a new SkillLoader instance with configured parser and renderer
     */
    public static SkillLoader create() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Collections.singletonList(YamlFrontMatterExtension.create()));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder(options).build();

        return new SkillLoader(parser, htmlRenderer);
    }

    /**
     * Traverses the direct subdirectories of the specified path, hunting for valid {@code SKILL.md} files.
     *
     * @param skillsDirectory The root directory containing multiple skill subfolders.
     * @return A loader instance populated with all successfully parsed skills.
     * @throws IOException If a fatal directory reading error occurs.
     */
    public static SkillLoader fromDirectory(Path skillsDirectory) throws IOException {
        SkillLoader loader = create();
        if (!Files.exists(skillsDirectory) || !Files.isDirectory(skillsDirectory)) {
            return loader;
        }

        try (Stream<Path> paths = Files.list(skillsDirectory)) {
            paths.filter(Files::isDirectory).forEach(dir -> {
                Path skillMd = dir.resolve("SKILL.md");
                if (Files.exists(skillMd) && Files.isRegularFile(skillMd)) {
                    try {
                        loader.skills.add(loader.parseSkillFile(skillMd));
                    } catch (Exception e) {
                        System.err.println(
                                "Failed to parse SKILL.md in directory " + dir.getFileName() + ": " + e.getMessage());
                    }
                }
            });
        }
        return loader;
    }

    private Skill parseSkillFile(Path file) throws IOException {
        String content = Files.readString(file);

        if (content.isBlank()) {
            throw new IllegalArgumentException("SKILL.md file is empty: " + file);
        }

        // Parse Markdown with flexmark
        Node document = parser.parse(content);

        // Extract YAML frontmatter
        Map<String, Object> metadata = extractFrontmatter(document);

        // Get content after frontmatter (instructions)
        String instructions = extractInstructions(content);

        // Extract required fields
        String name = metadata.containsKey("name") ? metadata.get("name").toString() : deriveSkillNameFromFile(file);

        if (name.isBlank()) {
            throw new IllegalArgumentException("Skill name cannot be empty in: " + file);
        }

        String description = metadata.containsKey("description")
                ? metadata.get("description").toString()
                : "A loaded agent skill";

        if (instructions.isBlank()) {
            throw new IllegalArgumentException("Skill instructions cannot be empty in: " + file);
        }

        return new Skill(name, description, document, instructions, metadata, file.getParent(), htmlRenderer);
    }

    private Map<String, Object> extractFrontmatter(Node document) {
        Map<String, Object> metadata = new HashMap<>();

        // YamlFrontMatterNode may not be direct children - need to traverse the AST
        // Using a visitor pattern to find all YamlFrontMatterNode instances
        document.getChildIterator().forEachRemaining(node -> {
            // Check if this node or its children contain YamlFrontMatterNode
            extractYamlNodes(node, metadata);
        });

        return metadata;
    }

    private void extractYamlNodes(Node node, Map<String, Object> metadata) {
        if (node instanceof YamlFrontMatterNode frontMatter) {
            String key = frontMatter.getKey();
            List<String> values = frontMatter.getValues();
            // value is a list of strings; handle multi-line values
            metadata.put(key, values.size() == 1 ? values.get(0) : values);
        }

        // Recursively check children
        if (node.hasChildren()) {
            node.getChildIterator().forEachRemaining(child -> extractYamlNodes(child, metadata));
        }
    }

    private static String deriveSkillNameFromFile(Path file) {
        Path parent = file.getParent();
        if (parent != null) {
            Path fileName = parent.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        }
        return "skill";
    }

    private String extractInstructions(String content) {
        // Find the end of frontmatter (second ---)
        int firstDash = content.indexOf("---");
        if (firstDash < 0) {
            return content.trim();
        }

        int secondDash = content.indexOf("---", firstDash + 3);
        if (secondDash < 0) {
            return content.trim();
        }

        // Return everything after second ---, trimmed
        return content.substring(secondDash + 3).trim();
    }

    /**
     * Returns the list of loaded skills.
     *
     * @return an unmodifiable list of all skills loaded by this loader
     */
    public List<Skill> getSkills() {
        return Collections.unmodifiableList(skills);
    }
}
