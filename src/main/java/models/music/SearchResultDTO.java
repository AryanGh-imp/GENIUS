package models.music;

import java.util.Objects;

/**
 * Data Transfer Object (DTO) for search results, representing an artist, album, or song.
 */
public class SearchResultDTO {
    /**
     * Enum representing the type of search result.
     */
    public enum ResultType {
        ARTIST, ALBUM, SONG
    }

    private final ResultType type;
    private final String name;
    private final String path;
    private final int views;


    public SearchResultDTO(ResultType type, String name, String path, int views) {
        this.type = type;
        this.name = name;
        this.path = path;
        this.views = views;
    }

    /**
     * Gets the type of the search result.
     *
     * @return The result type.
     */
    public ResultType getType() {
        return type;
    }

    /**
     * Gets the name of the artist, album, or song.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the file path associated with the result.
     *
     * @return The file path.
     */
    public String getPath() {
        return path;
    }

    public int getViews() {
        return views;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Path: %s, Views: %d)", type, name, path, views);
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