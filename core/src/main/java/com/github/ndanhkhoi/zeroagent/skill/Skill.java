package com.github.ndanhkhoi.zeroagent.skill;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.ast.Node;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a structured agent capability parsed from SKILL.md files.
 * <p>
 * Skills are reusable prompt templates defined in Markdown files with YAML frontmatter.
 * They enable agents to dynamically enhance their capabilities without code changes by
 * loading skill definitions at runtime. Skills are parsed using flexmark-java for robust
 * Markdown processing and YAML frontmatter extraction.
 *
 * <h2>Skill File Format</h2>
 * Skills are defined as Markdown files with YAML frontmatter:
 * <pre>{@code
 * ---
 * name: "code-reviewer"
 * description: "Reviews code for best practices and potential issues"
 * version: "1.0.0"
 * tags: ["code", "review", "analysis"]
 * author: "Your Name"
 * ---
 *
 * You are a code reviewer. Analyze the provided code for:
 * - Code style and formatting
 * - Potential bugs or issues
 * - Performance concerns
 * - Security vulnerabilities
 * - Best practice violations
 *
 * Provide constructive feedback and specific suggestions for improvement.
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Load skills from directory
 * SkillLoader skillLoader = new SkillLoader();
 * Map<String, Skill> skills = skillLoader.loadSkills("skills/");
 *
 * // Access a specific skill
 * Skill codeReviewer = skills.get("code-reviewer");
 *
 * // Get skill metadata
 * String name = codeReviewer.name();
 * String description = codeReviewer.description();
 * Optional<String> version = codeReviewer.getMetadata("version", String.class);
 *
 * // Get skill instructions
 * String instructions = codeReviewer.instructions();
 *
 * // Render as HTML
 * String html = codeReviewer.renderAsHtml();
 *
 * // Inject into agent prompt
 * String enhancedPrompt = skillLoader.injectSkills(basePrompt, skills);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * This class is immutable and thread-safe. Skill instances can be safely shared
 * across threads and sessions.
 *
 * @see SkillLoader
 */
public class Skill {
    /** The unique identifier of the skill (defaults to the folder name or frontmatter). */
    private final String name;

    /** A brief explanation of what the skill does. */
    private final String description;

    /** The flexmark AST node for advanced manipulation. */
    private final Node astNode;

    /** The raw markdown instructions text. */
    private final String instructions;

    /** Supplementary YAML frontmatter key-value pairs (rich types supported). */
    private final Map<String, Object> metadata;

    /** The absolute path to the directory hosting the SKILL.md and assets. */
    private final Path directory;

    /** The flexmark HTML renderer. */
    private final HtmlRenderer htmlRenderer;

    /**
     * Creates a new Skill instance.
     *
     * @param name the unique identifier of the skill
     * @param description a brief explanation of what the skill does
     * @param astNode the flexmark AST node
     * @param instructions the raw Markdown instructions text
     * @param metadata supplementary YAML frontmatter key-value pairs
     * @param directory the absolute path to the skill directory
     * @param htmlRenderer the flexmark HTML renderer
     */
    Skill(
            String name,
            String description,
            Node astNode,
            String instructions,
            Map<String, Object> metadata,
            Path directory,
            HtmlRenderer htmlRenderer) {
        this.name = name;
        this.description = description;
        this.astNode = astNode;
        this.instructions = instructions;
        this.metadata = metadata;
        this.directory = directory;
        this.htmlRenderer = htmlRenderer;
    }

    /**
     * Returns the unique identifier of the skill.
     *
     * @return the skill name
     */
    public String name() {
        return name;
    }

    /**
     * Returns a brief explanation of what the skill does.
     *
     * @return the skill description
     */
    public String description() {
        return description;
    }

    /**
     * Returns the raw Markdown instructions text.
     * Use {@link #renderAsHtml()} or {@link #getAstNode()} for processed content.
     *
     * @return the raw Markdown instructions
     */
    public String instructions() {
        return instructions;
    }

    /**
     * Returns the absolute path to the directory hosting the SKILL.md and assets.
     *
     * @return the skill directory path
     */
    public Path directory() {
        return directory;
    }

    /**
     * Returns the flexmark AST node for advanced manipulation.
     *
     * @return the AST root node
     */
    public Node getAstNode() {
        return astNode;
    }

    /**
     * Renders the skill instructions as HTML using flexmark.
     *
     * @return HTML representation of the instructions
     */
    public String renderAsHtml() {
        return htmlRenderer.render(astNode);
    }

    /**
     * Gets metadata value by key, supporting rich types (String, List, Map, etc.).
     *
     * @param <T> the type of the metadata value
     * @param key the metadata key
     * @param type the expected type class
     * @return an Optional containing the value if present and of the correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Gets all metadata as a read-only map.
     *
     * @return an immutable map of all metadata key-value pairs
     */
    public Map<String, Object> metadata() {
        return Map.copyOf(metadata);
    }
}
