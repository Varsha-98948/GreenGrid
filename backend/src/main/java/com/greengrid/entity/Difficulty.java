package com.greengrid.entity;

/**
 * Problem difficulty. Also determines which top-level folder
 * (Easy/ Medium/ Hard/) a solution is committed under in the
 * user's GitHub repository.
 */
public enum Difficulty {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    private final String folderName;

    Difficulty(String folderName) {
        this.folderName = folderName;
    }

    public String folderName() {
        return folderName;
    }
}
