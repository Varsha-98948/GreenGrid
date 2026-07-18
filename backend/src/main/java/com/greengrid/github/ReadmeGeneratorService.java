package com.greengrid.github;

import com.greengrid.entity.Problem;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Builds the human-readable README.md that lives next to every solution
 * in the user's repository. This is what makes the repo genuinely useful
 * to revisit later (and to recruiters browsing it) — not just a folder of
 * raw code.
 */
@Service
public class ReadmeGeneratorService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    public String generate(Problem problem) {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(problem.getTitle()).append("\n\n");

        md.append("| | |\n|---|---|\n");
        md.append("| **Platform** | ").append(problem.getPlatform()).append(" |\n");
        md.append("| **Difficulty** | ").append(problem.getDifficulty().folderName()).append(" |\n");
        if (problem.getProblemUrl() != null && !problem.getProblemUrl().isBlank()) {
            md.append("| **Problem Link** | [").append(problem.getProblemUrl()).append("](")
                    .append(problem.getProblemUrl()).append(") |\n");
        }
        md.append("| **Language** | ").append(problem.getLanguage()).append(" |\n");
        md.append("| **Solved On** | ").append(problem.getSolvedDate().format(DATE_FORMAT)).append(" |\n");

        if (!problem.getTags().isEmpty()) {
            String topics = problem.getTags().stream()
                    .map(t -> "`" + t.getName() + "`")
                    .collect(Collectors.joining(" "));
            md.append("\n## Topics\n\n").append(topics).append("\n");
        }

        if (problem.getNotes() != null && !problem.getNotes().isBlank()) {
            md.append("\n## Approach\n\n").append(problem.getNotes()).append("\n");
        }

        boolean hasTime = problem.getTimeComplexity() != null && !problem.getTimeComplexity().isBlank();
        boolean hasSpace = problem.getSpaceComplexity() != null && !problem.getSpaceComplexity().isBlank();
        if (hasTime || hasSpace) {
            md.append("\n## Complexity\n\n");
            if (hasTime) md.append("- **Time:** ").append(problem.getTimeComplexity()).append("\n");
            if (hasSpace) md.append("- **Space:** ").append(problem.getSpaceComplexity()).append("\n");
        }

        md.append("\n---\n*Tracked and committed automatically with [GreenGrid](https://greengrid.dev).*\n");

        return md.toString();
    }

    /** The commit message used for the README/solution/metadata triple. */
    public String buildCommitMessage(Problem problem) {
        return "Solve: " + problem.getTitle();
    }
}
