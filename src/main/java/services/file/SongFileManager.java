package services.file;

import models.account.Artist;
import models.music.Song;
import models.music.Album;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static utils.FileUtil.*;

public class SongFileManager extends FileManager {

    private String getAlbumDir(String artistNickName, String albumTitle) {
        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeAlbumTitle = sanitizeFileName(albumTitle);
        return DATA_DIR + "artists/" + safeArtistNickName + "/albums/" + safeAlbumTitle + "/";
    }

    private String getSongDir(String artistNickName, String songTitle, String albumName) {
        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeSongTitle = sanitizeFileName(songTitle);
        if (albumName != null && !albumName.isEmpty()) {
            return getAlbumDir(artistNickName, albumName) + safeSongTitle + "/";
        } else {
            return DATA_DIR + "artists/" + safeArtistNickName + "/singles/" + safeSongTitle + "/";
        }
    }

    public synchronized void saveSongsAndAlbumsForArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }

        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        File singlesDir = new File(artistDir + "singles/");
        File albumsDir = new File(artistDir + "albums/");

        // Get current songs and albums in the file system
        List<String> existingSingles = new ArrayList<>();
        List<String> existingAlbums = new ArrayList<>();

        // Identify existing singles
        if (singlesDir.exists() && singlesDir.isDirectory()) {
            File[] songFolders = singlesDir.listFiles(File::isDirectory);
            if (songFolders != null) {
                for (File songFolder : songFolders) {
                    existingSingles.add(songFolder.getName());
                }
            }
        }

        // Identify existing albums
        if (albumsDir.exists() && albumsDir.isDirectory()) {
            File[] albumFolders = albumsDir.listFiles(File::isDirectory);
            if (albumFolders != null) {
                for (File albumFolder : albumFolders) {
                    existingAlbums.add(albumFolder.getName());
                }
            }
        }

        // Remove singles that no longer exist
        List<String> currentSingles = artist.getSingles().stream()
                .map(Song::getTitle)
                .map(FileUtil::sanitizeFileName)
                .toList();
        for (String existingSingle : existingSingles) {
            if (!currentSingles.contains(existingSingle)) {
                deleteSong(artist.getNickName(), existingSingle, null);
            }
        }

        // Remove albums that no longer exist
        List<String> currentAlbums = artist.getAlbums().stream()
                .map(Album::getTitle)
                .map(FileUtil::sanitizeFileName)
                .toList();
        for (String existingAlbum : existingAlbums) {
            if (!currentAlbums.contains(existingAlbum)) {
                deleteAlbum(artist.getNickName(), existingAlbum);
            }
        }

        // Save modified singles
        List<String> artistNickNames = Collections.singletonList(artist.getNickName());
        for (Song single : artist.getSingles()) {
            if (single.isDirty()) {
                saveSong(artistNickNames, single.getTitle(), null, single.getLyrics(), single.getReleaseDate(), single.getLikes(), single.getViews(), single.getAlbumArtPath());
                single.setDirty(false);
            }
        }

        // Save modified albums
        for (Album album : artist.getAlbums()) {
            if (album.isDirty()) {
                List<String> songTitles = album.getSongs().stream()
                        .map(Song::getTitle)
                        .collect(Collectors.toList());
                saveAlbum(artist.getNickName(), album.getTitle(), album.getReleaseDate(), songTitles, album.getAlbumArtPath());
                album.setDirty(false);
            }
            for (Song song : album.getSongs()) {
                if (song.isDirty()) {
                    saveSong(artistNickNames, song.getTitle(), album.getTitle(), song.getLyrics(), song.getReleaseDate(), song.getLikes(), song.getViews(), song.getAlbumArtPath());
                    song.setDirty(false);
                }
            }
        }
    }

    public String loadLyrics(String metaFilePath) {
        String lyricsFilePath = metaFilePath.replace(".txt", "_lyrics.txt");
        File lyricsFile = new File(lyricsFilePath);
        if (lyricsFile.exists()) {
            List<String> lines = FileUtil.readFile(lyricsFilePath);
            return String.join("\n", lines);
        }
        return null;
    }

    public synchronized void saveSong(List<String> artistNickNames, String songTitle, String albumName, String lyrics, String releaseDate, int likes, int views, String albumArtPath) {
        if (artistNickNames == null || artistNickNames.isEmpty()) {
            throw new IllegalArgumentException("Artist nicknames list cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (lyrics == null) {
            throw new IllegalArgumentException("Lyrics cannot be null");
        }
        if (releaseDate == null || releaseDate.isEmpty()) {
            throw new IllegalArgumentException("Release date cannot be null or empty");
        }

        String artistNickName = artistNickNames.getFirst();
        String songDir = getSongDir(artistNickName, songTitle, albumName);
        ensureDataDirectoryExists(songDir);

        // Save track information (metadata file)
        String safeSongTitle = sanitizeFileName(songTitle);
        String songFile = songDir + safeSongTitle + ".txt";
        List<String> songData = new ArrayList<>();
        songData.add("Song Name: " + songTitle);
        songData.add("Artists: " + String.join(",", artistNickNames));
        songData.add("Likes: " + likes);
        songData.add("Views: " + views);
        songData.add("Release Date: " + releaseDate);
        if (albumArtPath != null) {
            songData.add("AlbumArtPath: " + albumArtPath);
        }
        try {
            writeFile(songFile, songData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save song file: " + songFile, e);
        }

        // Save lyrics in a separate file
        String lyricsFile = songDir + safeSongTitle + "_lyrics.txt";
        try {
            writeFile(lyricsFile, Collections.singletonList(lyrics));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save lyrics file: " + lyricsFile, e);
        }

        // Save lyrics history
        String lyricsHistoryFile = songDir + safeSongTitle + "-lyrics-history.txt";
        List<String> lyricsHistory = new ArrayList<>();
        File historyFile = new File(lyricsHistoryFile);
        if (historyFile.exists()) {
            lyricsHistory = readFile(lyricsHistoryFile);
        }
        lyricsHistory.add("Lyrics: " + lyrics + " | Timestamp: " + System.currentTimeMillis());
        try {
            writeFile(lyricsHistoryFile, lyricsHistory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save lyrics history file: " + lyricsHistoryFile, e);
        }
    }

    public synchronized void saveAlbum(String artistNickName, String albumTitle, String releaseDate, List<String> songTitles, String albumArtPath) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (albumTitle == null || albumTitle.isEmpty()) {
            throw new IllegalArgumentException("Album title cannot be null or empty");
        }
        if (releaseDate == null) {
            throw new IllegalArgumentException("Release date cannot be null");
        }

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        ensureDataDirectoryExists(albumDir);

        String albumFile = albumDir + "album.txt";
        List<String> albumData = new ArrayList<>();
        albumData.add("Album Title: " + albumTitle);
        albumData.add("Release Date: " + releaseDate);
        albumData.add("Songs: " + (songTitles != null ? String.join(",", songTitles) : ""));
        if (albumArtPath != null && !albumArtPath.isEmpty()) {
            albumData.add("AlbumArtPath: " + albumArtPath);
        }
        try {
            writeFile(albumFile, albumData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save album file: " + albumFile, e);
        }
    }

    public synchronized void approveLyricEdit(String artistNickName, String songTitle, String albumName, String suggestedLyrics) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (suggestedLyrics == null) {
            throw new IllegalArgumentException("Suggested lyrics cannot be null");
        }

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        String safeSongTitle = sanitizeFileName(songTitle);
        String lyricsFile = songDir + safeSongTitle + "_lyrics.txt";
        File file = new File(lyricsFile);
        if (!file.exists()) {
            throw new IllegalStateException("Lyrics file not found: " + lyricsFile);
        }

        // Update the lyrics file
        try {
            writeFile(lyricsFile, Collections.singletonList(suggestedLyrics));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update lyrics file: " + lyricsFile, e);
        }

        // Update lyrics history
        String lyricsHistoryFile = songDir + safeSongTitle + "-lyrics-history.txt";
        List<String> lyricsHistory = new ArrayList<>();
        File historyFile = new File(lyricsHistoryFile);
        if (historyFile.exists()) {
            lyricsHistory = readFile(lyricsHistoryFile);
        }
        lyricsHistory.add("Lyrics: " + suggestedLyrics + " | Timestamp: " + System.currentTimeMillis());
        try {
            writeFile(lyricsHistoryFile, lyricsHistory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update lyrics history file: " + lyricsHistoryFile, e);
        }
    }

    public synchronized void addComment(String artistNickName, String songTitle, String albumName, String commentText, String user) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (commentText == null || commentText.isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be null or empty");
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("User cannot be null or empty");
        }

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        File songDirFile = new File(songDir);
        if (!songDirFile.exists()) {
            throw new IllegalStateException("Song directory not found: " + songDir);
        }

        String commentsFile = songDir + sanitizeFileName(songTitle) + "-comments.txt";
        List<String> comments = new ArrayList<>();
        File commentsFileObj = new File(commentsFile);
        if (commentsFileObj.exists()) {
            comments = readFile(commentsFile);
        }
        String escapedCommentText = commentText.replace("|", "\\|");
        String escapedUser = user.replace("|", "\\|");
        comments.add("Comment: " + escapedCommentText + " | User: " + escapedUser + " | Timestamp: " + System.currentTimeMillis());
        try {
            writeFile(commentsFile, comments);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save comment to file: " + commentsFile, e);
        }
    }

    public synchronized List<String[]> loadComments(String artistNickName, String songTitle, String albumName) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        File songDirFile = new File(songDir);
        if (!songDirFile.exists()) {
            return new ArrayList<>();
        }

        String commentsFile = songDir + sanitizeFileName(songTitle) + "-comments.txt";
        File commentsFileObj = new File(commentsFile);
        if (!commentsFileObj.exists()) {
            return new ArrayList<>();
        }

        List<String[]> comments = new ArrayList<>();
        List<String> lines = readFile(commentsFile);
        for (String line : lines) {
            String[] parts = line.split(" \\| ");
            if (parts.length != 3) {
                throw new IllegalStateException("Invalid comment format in file: " + commentsFile + ", line: " + line);
            }
            String commentText = parts[0].substring("Comment: ".length()).replace("\\|", "|");
            String user = parts[1].substring("User: ".length()).replace("\\|", "|");
            String timestamp = parts[2].substring("Timestamp: ".length());
            comments.add(new String[]{commentText, user, timestamp});
        }
        return comments;
    }

    public synchronized List<String[]> loadLyricsHistory(String artistNickName, String songTitle, String albumName) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        File songDirFile = new File(songDir);
        if (!songDirFile.exists()) {
            return new ArrayList<>();
        }

        String lyricsHistoryFile = songDir + sanitizeFileName(songTitle) + "-lyrics-history.txt";
        File historyFile = new File(lyricsHistoryFile);
        if (!historyFile.exists()) {
            return new ArrayList<>();
        }

        List<String[]> history = new ArrayList<>();
        List<String> lines = readFile(lyricsHistoryFile);
        for (String line : lines) {
            String[] parts = line.split(" \\| ");
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid lyrics history format in file: " + lyricsHistoryFile + ", line: " + line);
            }
            String lyrics = parts[0].substring("Lyrics: ".length());
            String timestamp = parts[1].substring("Timestamp: ".length());
            history.add(new String[]{lyrics, timestamp});
        }
        return history;
    }

    public void loadSongsAndAlbumsForArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }

        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        File artistDirFile = new File(artistDir);
        if (!artistDirFile.exists()) {
            return;
        }

        File singlesDir = new File(artistDir + "singles/");
        File albumsDir = new File(artistDir + "albums/");

        // Load singles
        if (singlesDir.exists() && singlesDir.isDirectory()) {
            File[] songFolders = singlesDir.listFiles(File::isDirectory);
            if (songFolders != null) {
                for (File songFolder : songFolders) {
                    File songFile = new File(songFolder, songFolder.getName() + ".txt");
                    if (songFile.exists()) {
                        List<String> songData = readFile(songFile.getPath());
                        String lyrics = loadLyrics(songFile.getPath());
                        Song song = parseSongFromFile(songData, null, lyrics);
                        List<String> artistNickNames = parseArtistNickNames(songData);
                        for (String nickName : artistNickNames) {
                            if (nickName.equals(artist.getNickName())) {
                                song.addArtist(artist);
                            }
                        }
                        artist.getSingles().add(song);
                    }
                }
            }
        }

        // Load albums
        if (albumsDir.exists() && albumsDir.isDirectory()) {
            File[] albumFolders = albumsDir.listFiles(File::isDirectory);
            if (albumFolders != null) {
                for (File albumFolder : albumFolders) {
                    String albumFilePath = albumFolder.getPath() + "/album.txt";
                    File albumFile = new File(albumFilePath);
                    String albumTitle = albumFolder.getName();
                    String releaseDate = "Not set";
                    String albumArtPath = null;
                    List<String> songTitles = new ArrayList<>();

                    if (albumFile.exists()) {
                        List<String> albumData = readFile(albumFilePath);
                        for (String line : albumData) {
                            int index = line.indexOf(": ");
                            if (index != -1) {
                                String key = line.substring(0, index);
                                String value = line.substring(index + 2);
                                if (key.equals("Release Date")) {
                                    releaseDate = value;
                                } else if (key.equals("Songs") && !value.isEmpty()) {
                                    songTitles = List.of(value.split(","));
                                } else if (key.equals("AlbumArtPath")) {
                                    albumArtPath = value;
                                }
                            }
                        }
                    }

                    Album album = new Album(albumTitle, releaseDate, artist);
                    if (albumArtPath != null) {
                        album.setAlbumArtPath(albumArtPath);
                    }
                    File[] songFolders = albumFolder.listFiles(File::isDirectory);
                    if (songFolders != null) {
                        for (File songFolder : songFolders) {
                            File songFile = new File(songFolder, songFolder.getName() + ".txt");
                            if (songFile.exists()) {
                                List<String> songData = readFile(songFile.getPath());
                                String lyrics = loadLyrics(songFile.getPath());
                                Song song = parseSongFromFile(songData, album, lyrics);
                                List<String> artistNickNames = parseArtistNickNames(songData);
                                for (String nickName : artistNickNames) {
                                    if (nickName.equals(artist.getNickName())) {
                                        song.addArtist(artist);
                                    }
                                }
                                album.addSong(song);
                            }
                        }
                    }
                    artist.getAlbums().add(album);
                }
            }
        }
    }

    public synchronized void deleteSong(String artistNickName, String songTitle, String albumName) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        File songDirFile = new File(songDir);
        if (!songDirFile.exists()) {
            throw new IllegalStateException("Song directory not found: " + songDir);
        }

        // Get the album art path before deleting the song directory
        String albumArtPath = null;
        File songFile = new File(songDir + sanitizeFileName(songTitle) + ".txt");
        if (songFile.exists()) {
            List<String> songData = readFile(songFile.getPath());
            for (String line : songData) {
                if (line.startsWith("AlbumArtPath: ")) {
                    albumArtPath = line.substring("AlbumArtPath: ".length());
                    break;
                }
            }
        }

        // Delete the song directory
        FileUtil.deleteDirectory(songDirFile);

        // Delete the album art file if it exists
        if (albumArtPath != null && !albumArtPath.isEmpty()) {
            File albumArtFile = new File(albumArtPath);
            if (albumArtFile.exists()) {
                albumArtFile.delete();
            }
        }

        // If the song belongs to an album, remove it from the album's song list
        if (albumName != null && !albumName.isEmpty()) {
            String albumDir = getAlbumDir(artistNickName, albumName);
            File albumFile = new File(albumDir + "album.txt");
            if (albumFile.exists()) {
                List<String> albumData = readFile(albumFile.getPath());
                List<String> updatedAlbumData = new ArrayList<>();
                for (String line : albumData) {
                    if (line.startsWith("Songs: ") && !line.equals("Songs: ")) {
                        List<String> songTitles = new ArrayList<>(List.of(line.substring("Songs: ".length()).split(",")));
                        songTitles.remove(songTitle);
                        updatedAlbumData.add("Songs: " + String.join(",", songTitles));
                    } else {
                        updatedAlbumData.add(line);
                    }
                }
                try {
                    writeFile(albumFile.getPath(), updatedAlbumData);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to update album file: " + albumFile.getPath(), e);
                }
            }
        }
    }

    public synchronized void deleteAlbum(String artistNickName, String albumTitle) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (albumTitle == null || albumTitle.isEmpty()) {
            throw new IllegalArgumentException("Album title cannot be null or empty");
        }

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        File albumDirFile = new File(albumDir);
        if (!albumDirFile.exists()) {
            throw new IllegalStateException("Album directory not found: " + albumDir);
        }

        // Get the album art path before deleting the album directory
        String albumArtPath = null;
        File albumFile = new File(albumDir + "album.txt");
        if (albumFile.exists()) {
            List<String> albumData = readFile(albumFile.getPath());
            for (String line : albumData) {
                if (line.startsWith("AlbumArtPath: ")) {
                    albumArtPath = line.substring("AlbumArtPath: ".length());
                    break;
                }
            }
        }

        // Delete the album directory
        FileUtil.deleteDirectory(albumDirFile);

        // Delete the album art file if it exists
        if (albumArtPath != null && !albumArtPath.isEmpty()) {
            File albumArtFile = new File(albumArtPath);
            if (albumArtFile.exists()) {
                albumArtFile.delete();
            }
        }
    }

    public synchronized String saveAlbumArt(String artistNickName, String albumTitle, File imageFile) throws IOException {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (albumTitle == null || albumTitle.isEmpty()) {
            throw new IllegalArgumentException("Album title cannot be null or empty");
        }
        if (imageFile == null || !imageFile.exists()) {
            throw new IllegalArgumentException("Image file cannot be null or does not exist");
        }

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        ensureDataDirectoryExists(albumDir);
        String fileExtension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String artFile = albumDir + "album_art" + fileExtension;
        Files.copy(imageFile.toPath(), Paths.get(artFile), StandardCopyOption.REPLACE_EXISTING);

        // Update album.txt with the album art path
        File albumFile = new File(albumDir + "album.txt");
        if (!albumFile.exists()) {
            throw new IllegalStateException("album.txt not found in: " + albumDir);
        }

        List<String> albumData = readFile(albumFile.getPath());
        List<String> updatedAlbumData = new ArrayList<>();
        boolean albumArtPathUpdated = false;

        for (String line : albumData) {
            if (line.startsWith("AlbumArtPath: ")) {
                updatedAlbumData.add("AlbumArtPath: " + artFile);
                albumArtPathUpdated = true;
            } else {
                updatedAlbumData.add(line);
            }
        }

        if (!albumArtPathUpdated) {
            updatedAlbumData.add("AlbumArtPath: " + artFile);
        }

        try {
            writeFile(albumFile.getPath(), updatedAlbumData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update album file with album art path: " + albumFile.getPath(), e);
        }

        return artFile; // مسیر تصویر را برمی‌گرداند
    }

    private Song parseSongFromFile(List<String> songData, Album album, String lyrics) {
        String title = null;
        String releaseDate = "Not set";
        int likes = 0;
        int views = 0;
        String albumArtPath = null;

        for (String line : songData) {
            int index = line.indexOf(": ");
            if (index != -1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 2);
                switch (key) {
                    case "Song Name": title = value; break;
                    case "Likes":
                        try {
                            likes = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            throw new IllegalStateException("Invalid Likes value: " + value, e);
                        }
                        break;
                    case "Views":
                        try {
                            views = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            throw new IllegalStateException("Invalid Views value: " + value, e);
                        }
                        break;
                    case "Release Date": releaseDate = value; break;
                    case "AlbumArtPath": albumArtPath = value; break;
                }
            }
        }

        if (title == null) {
            throw new IllegalStateException("Failed to parse song: Song title is missing in data: " + songData);
        }

        Song song = new Song(title, lyrics != null ? lyrics : "", releaseDate);
        song.setLikes(likes);
        song.setViews(views);
        if (album != null) {
            song.setAlbum(album);
        }
        if (albumArtPath != null) {
            song.setAlbumArtPath(albumArtPath);
        }
        song.setDirty(false);
        return song;
    }

    private List<String> parseArtistNickNames(List<String> songData) {
        for (String line : songData) {
            if (line.startsWith("Artists: ")) {
                String artistsStr = line.substring("Artists: ".length());
                return List.of(artistsStr.split(","));
            }
        }
        return new ArrayList<>();
    }
}