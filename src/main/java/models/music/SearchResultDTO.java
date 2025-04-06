package models.music;

import java.util.Arrays;
import java.util.Objects;

public class SearchResultDTO {
    public enum ResultType {
        ARTIST, SONG, ALBUM
    }

    private final ResultType type;
    private final String name;
    private final String path;

    public SearchResultDTO(String type, String name, String path) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        try {
            this.type = ResultType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid type: " + type + ". Must be one of: " +
                    String.join(", ", Arrays.stream(ResultType.values()).map(Enum::name).toList()));
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        this.name = name;
        this.path = path;
    }

    public SearchResultDTO(ResultType type, String name, String path) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        this.type = type;
        this.name = name;
        this.path = path;
    }

    // Getters
    public ResultType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return type.name() + ": " + name + " (Path: " + path + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResultDTO that = (SearchResultDTO) o;
        return type == that.type &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, path);
    }
}