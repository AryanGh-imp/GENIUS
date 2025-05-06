package services;

import models.DTO.ChartEntryDTO;
import models.account.Artist;
import models.music.Song;
import models.DTO.SongDTO;
import services.file.ArtistFileManager;
import services.file.SongFileManager;

import java.util.ArrayList;
import java.util.List;

public class ChartService {
    private final ArtistFileManager artistFileManager;
    private final SongFileManager songFileManager;

    public ChartService(ArtistFileManager artistFileManager, SongFileManager songFileManager) {
        this.artistFileManager = artistFileManager;
        this.songFileManager = songFileManager;
    }

    public List<ChartEntryDTO> getTopSongsChart() {
        // Step 1: Load all artists
        List<Artist> artists = artistFileManager.loadAllArtists();

        // Step 2: Collect all songs from artists using SongFileManager
        List<SongDTO> allSongs = new ArrayList<>();
        for (Artist artist : artists) {
            songFileManager.loadSongsAndAlbumsForArtist(artist); // Load songs and albums
            List<Song> songs = new ArrayList<>(artist.getSingles());
            artist.getAlbums().forEach(album -> songs.addAll(album.getSongs()));

            // Convert each song to SongDTO
            allSongs.addAll(songs.stream()
                    .map(song -> getSongDTO(artist, song))
                    .toList());
        }

        // TODO: Sort songs by likes in the future

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
        String metaFilePath = song.getMetaFilePath();

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
}