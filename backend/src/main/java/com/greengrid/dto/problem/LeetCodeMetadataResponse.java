package com.greengrid.dto.problem;

import java.util.List;

public record LeetCodeMetadataResponse(boolean found, String title, String difficulty, List<String> topics) {
}
