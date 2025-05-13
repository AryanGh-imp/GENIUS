package services;

import models.DTO.ChartEntryDTO;
import models.account.Artist;
import models.music.Song;
import models.DTO.SongDTO;
import services.file.ArtistFileManager;
import services.file.SongFileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartService {
    private final ArtistFileManager artistFileManager;
    private final SongFileManager songFileManager;

    // Cache for songs
    private final Map<String, List<Song>> songCache = new HashMap<>();

    public ChartService(ArtistFileManager artistFileManager, SongFileManager songFileManager) {
        if (artistFileManager == null) {
            throw new IllegalArgumentException("ArtistFileManager cannot be null");
        }
        if (songFileManager == null) {
            throw new IllegalArgumentException("SongFileManager cannot be null");
        }
        this.artistFileManager = artistFileManager;
        this.songFileManager = songFileManager;
    }

    public List<ChartEntryDTO> getTopSongsChart() {
        // Step 1: Load all artists
        List<Artist> artists = artistFileManager.loadAllArtists();

        // Step 2: Collect all songs from approved artists using cache or load if needed
        List<SongDTO> allSongs = new ArrayList<>();
        for (Artist artist : artists) {
            if (!artist.isApproved()) {
                continue;
            }
            try {
                List<Song> songs = songCache.get(artist.getNickName());
                if (songs == null || songs.isEmpty()) {
                    songFileManager.loadSongsAndAlbumsForArtist(artist, artistFileManager);
                    songs = new ArrayList<>(artist.getSingles());
                    List<Song> finalSongs = songs;
                    artist.getAlbums().forEach(album -> finalSongs.addAll(album.getSongs()));
                    songCache.put(artist.getNickName(), new ArrayList<>(finalSongs));
                }

                allSongs.addAll(songs.stream()
                        .map(song -> getSongDTO(artist, song))
                        .toList());
            } catch (Exception e) {
                System.err.println("Failed to load songs for artist '" + artist.getNickName() + "': " + e.getMessage());
            }
        }

        // Step 3: Sort songs by views in descending order using Stream API
        List<SongDTO> sortedSongs = allSongs.stream()
                .sorted((s1, s2) -> Integer.compare(s2.views(), s1.views()))
                .toList();

        // Step 4: Convert to ChartEntryDTO with ranking
        List<ChartEntryDTO> chartEntries = new ArrayList<>();
        int rank = 1;
        for (SongDTO song : sortedSongs) {
            ChartEntryDTO entry = new ChartEntryDTO(
                    rank++,
                    song.title(),
                    song.artistName(),
                    String.valueOf(song.views()),
                    String.valueOf(song.likes())
            );
            chartEntries.add(entry);
        }

        return chartEntries;
    }

    private static SongDTO getSongDTO(Artist artist, Song song) {
        String albumName = song.getAlbum() != null ? song.getAlbum().getTitle() : null;
        String artistName = artist.getNickName();
        String albumArtPath = song.getAlbumArtPath() != null ? song.getAlbumArtPath() : "GENIUS/src/main/resources/pics/Genius.com_logo_yellow.png";
        String metaFilePath = song.getMetaFilePath() != null ? song.getMetaFilePath() : "";

        return new SongDTO(
                song.getTitle(),
                artistName,
                albumName,
                song.getViews(),
                song.getLikes(),
                metaFilePath,
                song.getReleaseDate(),
                albumArtPath
        );
    }

    // Method to clear cache
    public void clearCache() {
        songCache.clear();
    }
}