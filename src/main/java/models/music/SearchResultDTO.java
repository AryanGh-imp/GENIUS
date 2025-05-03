package models.music;

public record SearchResultDTO(ResultType type, String name, String path, int views) {
    public enum ResultType {
        ARTIST, ALBUM, SONG
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Path: %s, Views: %d)", type, name, path, views);
    }
}