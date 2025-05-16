package models.DTO;

public record SearchResultDTO(ResultType type, String name, String path, int views, int likes) {
    public enum ResultType {
        ARTIST, ALBUM, SONG
    }

    public SearchResultDTO {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        if (views < 0) {
            throw new IllegalArgumentException("Views cannot be negative");
        }
        if (likes < 0) {
            throw new IllegalArgumentException("Likes cannot be negative");
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Path: %s, Views: %d, Likes: %d)", type, name, path, views, likes);
    }
}