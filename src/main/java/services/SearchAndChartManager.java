package services;

import models.music.SearchResultDTO;
import models.music.SongDTO;
import utils.FileUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A manager class responsible for handling search operations and generating charts for songs.
 * It loads all songs at startup and provides methods to search for artists, albums, and songs,
 * as well as to retrieve top songs based on views.
 */
public class SearchAndChartManager {
    // A read-only list of all songs loaded at startup
    private final List<SongDTO> allSongs;

    /**
     * Constructor that initializes the SearchAndChartManager by loading all songs.
     * The list of songs is made unmodifiable to prevent accidental changes.
     */
    public SearchAndChartManager() {
        this.allSongs = Collections.unmodifiableList(loadAllSongs());
    }

    /**
     * Loads all songs from the artists' directories at startup.
     * This method scans the "artists" directory, including singles and albums,
     * and creates a list of SongDTO objects.
     *
     * @return A list of SongDTO objects representing all songs.
     */
    private List<SongDTO> loadAllSongs() {
        List<SongDTO> songs = new ArrayList<>();
        File artistsDir = new File(FileUtil.DATA_DIR + "artists/");

        // Check if the artists directory exists and is a directory
        if (!artistsDir.exists() || !artistsDir.isDirectory()) {
            return songs;
        }

        // Iterate through each artist directory
        for (File artistDir : Objects.requireNonNull(artistsDir.listFiles(File::isDirectory))) {
            String artistName = artistDir.getName();
            File singlesDir = new File(artistDir, "singles/");
            File albumsDir = new File(artistDir, "albums/");

            // Load single songs (not part of an album)
            if (singlesDir.exists() && singlesDir.isDirectory()) {
                for (File songDir : Objects.requireNonNull(singlesDir.listFiles(File::isDirectory))) {
                    SongDTO song = loadSongFromFile(songDir, artistName, null);
                    if (song != null) {
                        songs.add(song);
                    }
                }
            }

            // Load albums and their songs
            if (albumsDir.exists() && albumsDir.isDirectory()) {
                for (File albumDir : Objects.requireNonNull(albumsDir.listFiles(File::isDirectory))) {
                    String albumName = albumDir.getName();
                    for (File songDir : Objects.requireNonNull(albumDir.listFiles(File::isDirectory))) {
                        SongDTO song = loadSongFromFile(songDir, artistName, albumName);
                        if (song != null) {
                            songs.add(song);
                        }
                    }
                }
            }
        }
        return songs;
    }

    /**
     * Helper method to load a single song from its metadata file.
     * This method reads the metadata file for a song and creates a SongDTO object.
     *
     * @param songDir    The directory containing the song's metadata file.
     * @param artistName The name of the artist.
     * @param albumName  The name of the album (null for singles).
     * @return A SongDTO object representing the song, or null if loading fails.
     */
    private SongDTO loadSongFromFile(File songDir, String artistName, String albumName) {
        String metaFilePath = songDir.getPath() + "/" + songDir.getName() + ".txt";
        File metaFile = new File(metaFilePath);

        // Check if the metadata file exists
        if (!metaFile.exists()) {
            System.err.println("Metadata file does not exist: " + metaFilePath);
            return null;
        }

        try {
            List<String> metaData = FileUtil.readFile(metaFilePath);
            String title = null;
            Integer views = null;

            // Parse the metadata file to extract song details
            for (String line : metaData) {
                int index = line.indexOf(": ");
                if (index != -1) {
                    String key = line.substring(0, index);
                    String value = line.substring(index + 2);
                    switch (key) {
                        case "Song Name":
                            title = value;
                            break;
                        case "Views":
                            views = Integer.parseInt(value);
                            break;
                    }
                }
            }

            // Validate that required fields are present
            if (title == null) {
                System.err.println("Song title is missing in file: " + metaFilePath);
                return null;
            }
            if (views == null) {
                System.err.println("Views are missing in file: " + metaFilePath);
                return null;
            }

            // Create and return a new SongDTO object
            return new SongDTO(title, artistName, albumName, views, metaFilePath);
        } catch (NumberFormatException e) {
            System.err.println("Invalid views format in file: " + metaFilePath + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Failed to load song from file: " + metaFilePath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the top songs based on the number of views.
     *
     * @param limit The maximum number of songs to return.
     * @return A list of SongDTO objects sorted by views in descending order, limited to the specified number.
     */
    public List<SongDTO> getTopSongs(int limit) {
        // Return an empty list if the limit is invalid or no songs are available
        if (limit <= 0 || allSongs.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort songs by views in descending order and limit the result
        return allSongs.stream()
                .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Searches for artists, albums, and songs based on a query.
     * The search is case-insensitive and matches the query against artist names, album names, and song titles.
     *
     * @param query The search query.
     * @return A list of SearchResultDTO objects representing the matching artists, albums, and songs.
     */
    public List<SearchResultDTO> search(String query) {
        List<SearchResultDTO> results = new ArrayList<>();

        // Return an empty list if the query is null or empty
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        query = query.toLowerCase();

        // Search through the list of all songs
        for (SongDTO song : allSongs) {
            // Search for artists
            if (song.getArtistName().toLowerCase().contains(query)) {
                String artistPath = FileUtil.DATA_DIR + "artists/" + song.getArtistName();
                // Avoid duplicate artist results
                if (!results.stream().anyMatch(r -> r.getType() == SearchResultDTO.ResultType.ARTIST && r.getName().equals(song.getArtistName()))) {
                    results.add(new SearchResultDTO(SearchResultDTO.ResultType.ARTIST, song.getArtistName(), artistPath));
                }
            }

            // Search for albums
            if (song.getAlbumName() != null && song.getAlbumName().toLowerCase().contains(query)) {
                String albumPath = FileUtil.DATA_DIR + "artists/" + song.getArtistName() + "/albums/" + song.getAlbumName();
                // Avoid duplicate album results
                if (!results.stream().anyMatch(r -> r.getType() == SearchResultDTO.ResultType.ALBUM && r.getName().equals(song.getAlbumName()))) {
                    results.add(new SearchResultDTO(SearchResultDTO.ResultType.ALBUM, song.getAlbumName(), albumPath));
                }
            }

            // Search for songs
            if (song.getTitle().toLowerCase().contains(query)) {
                results.add(new SearchResultDTO(SearchResultDTO.ResultType.SONG, song.getTitle(), song.getMetaFilePath()));
            }
        }

        return results;
    }
}