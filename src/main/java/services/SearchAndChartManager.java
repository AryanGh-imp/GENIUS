package services;

import models.DTO.ChartEntryDTO;
import models.account.Artist;
import models.DTO.SearchResultDTO;
import models.music.Song;
import models.DTO.SongDTO;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import utils.FileUtil;

import java.util.*;

public class SearchAndChartManager {
    private final List<SongDTO> allSongs;
    private final ArtistFileManager artistFileManager;
    private final SongFileManager songFileManager;
    private final ChartService chartService;

    public SearchAndChartManager(ArtistFileManager artistFileManager, SongFileManager songFileManager) {
        this.artistFileManager = artistFileManager;
        this.songFileManager = songFileManager;
        this.chartService = new ChartService(artistFileManager, songFileManager);
        this.allSongs = loadAllSongs();
    }

    private List<SongDTO> loadAllSongs() {
        List<SongDTO> songs = new ArrayList<>();
        List<Artist> artists = artistFileManager.loadAllArtists();

        for (Artist artist : artists) {
            songFileManager.loadSongsAndAlbumsForArtist(artist);
            List<Song> artistSongs = new ArrayList<>(artist.getSingles());
            for (var album : artist.getAlbums()) {
                artistSongs.addAll(album.getSongs());
            }

            for (Song song : artistSongs) {
                songs.add(new SongDTO(
                        song.getTitle(),
                        artist.getNickName(),
                        song.getAlbum() != null ? song.getAlbum().getTitle() : null,
                        song.getViews(),
                        song.getLikes(),
                        song.getMetaFilePath(),
                        song.getReleaseDate(),
                        song.getAlbumArtPath() != null ? song.getAlbumArtPath() : "GENIUS/src/main/resources/pics/Genius.com_logo_yellow.png"
                ));
            }
        }
        return songs;
    }

    public String loadLyrics(String metaFilePath) {
        String lyricsFilePath = metaFilePath.replace(".txt", "_lyrics.txt");
        try {
            return songFileManager.loadLyrics(metaFilePath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load lyrics for file: " + lyricsFilePath, e);
        }
    }

    public List<SongDTO> getTopSongs(int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<ChartEntryDTO> chartEntries = chartService.getTopSongsChart();
        List<SongDTO> topSongs = new ArrayList<>();
        int count = 0;

        for (ChartEntryDTO entry : chartEntries) {
            if (count >= limit) {
                break;
            }
            for (SongDTO song : allSongs) {
                if (song.title().equals(entry.songTitle()) && song.artistName().equals(entry.artist())) {
                    topSongs.add(song);
                    count++;
                    break;
                }
            }
        }
        return topSongs;
    }

    public List<SongDTO> getAllSongs() {
        return new ArrayList<>(allSongs);
    }

    public List<SearchResultDTO> search(String query) {
        List<SearchResultDTO> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        query = query.toLowerCase();

        // To store total views and likes for artists and albums
        Map<String, Integer> artistViews = new HashMap<>();
        Map<String, Integer> artistLikes = new HashMap<>();
        Map<String, Integer> albumViews = new HashMap<>();
        Map<String, Integer> albumLikes = new HashMap<>();
        Map<String, String> albumArtists = new HashMap<>();

        // Search all songs
        for (SongDTO song : allSongs) {
            String artistName = song.artistName();
            String albumName = song.albumName();

            // Gathering information for the artist
            artistViews.put(artistName, artistViews.getOrDefault(artistName, 0) + song.views());
            artistLikes.put(artistName, artistLikes.getOrDefault(artistName, 0) + song.likes());

            // Gathering information for the album
            if (albumName != null) {
                albumViews.put(albumName, albumViews.getOrDefault(albumName, 0) + song.views());
                albumLikes.put(albumName, albumLikes.getOrDefault(albumName, 0) + song.likes());
                albumArtists.putIfAbsent(albumName, artistName);
            }

            // Search for songs
            if (song.title().toLowerCase().contains(query)) {
                results.add(new SearchResultDTO(
                        SearchResultDTO.ResultType.SONG,
                        song.title(),
                        song.metaFilePath(),
                        song.views(),
                        song.likes()
                ));
            }
        }

        // Search for artists
        for (String artistName : artistViews.keySet()) {
            if (artistName.toLowerCase().contains(query)) {
                String artistPath = FileUtil.DATA_DIR + "artists/" + artistName;
                results.add(new SearchResultDTO(
                        SearchResultDTO.ResultType.ARTIST,
                        artistName,
                        artistPath,
                        artistViews.get(artistName),
                        artistLikes.get(artistName)
                ));
            }
        }

        // Search for albums
        for (String albumName : albumViews.keySet()) {
            if (albumName.toLowerCase().contains(query)) {
                String artistName = albumArtists.get(albumName);
                String albumPath = FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumName;
                results.add(new SearchResultDTO(
                        SearchResultDTO.ResultType.ALBUM,
                        albumName,
                        albumPath,
                        albumViews.get(albumName),
                        albumLikes.get(albumName)
                ));
            }
        }

        return results;
    }
}