package com.greengrid.github;

import java.util.Map;

public final class LanguageExtensionMapper {

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("java", "java"),
            Map.entry("python", "py"),
            Map.entry("javascript", "js"),
            Map.entry("typescript", "ts"),
            Map.entry("c++", "cpp"),
            Map.entry("cpp", "cpp"),
            Map.entry("c", "c"),
            Map.entry("c#", "cs"),
            Map.entry("csharp", "cs"),
            Map.entry("go", "go"),
            Map.entry("golang", "go"),
            Map.entry("rust", "rs"),
            Map.entry("kotlin", "kt"),
            Map.entry("swift", "swift"),
            Map.entry("ruby", "rb"),
            Map.entry("php", "php"),
            Map.entry("scala", "scala"),
            Map.entry("sql", "sql")
    );

    private LanguageExtensionMapper() {}

    public static String extensionFor(String language) {
        return EXTENSIONS.getOrDefault(language.trim().toLowerCase(), "txt");
    }
}
