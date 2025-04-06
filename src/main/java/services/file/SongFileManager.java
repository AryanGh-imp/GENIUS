package services.file;

import models.account.Artist;
import models.music.Song;
import models.music.Album;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public synchronized void saveSong(List<String> artistNickNames, String songTitle, String albumName, String lyrics, File sourceFile, String releaseDate) {
        if (artistNickNames == null || artistNickNames.isEmpty()) {
            throw new IllegalArgumentException("Artist nicknames list cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Source file cannot be null and must exist");
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

        String safeSongTitle = sanitizeFileName(songTitle);
        String songFile = songDir + safeSongTitle + ".txt";
        List<String> songData = new ArrayList<>();
        songData.add("Song Name: " + songTitle);
        songData.add("Artists: " + String.join(",", artistNickNames));
        songData.add("Likes: 0");
        songData.add("Views: 0");
        songData.add("Release Date: " + releaseDate);
        songData.add("Lyrics: " + lyrics);
        songData.add("File Path: " + sourceFile.getPath());
        try {
            writeFile(songFile, songData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save song file: " + songFile, e);
        }

        String lyricsHistoryFile = songDir + safeSongTitle + "-lyrics-history.txt";
        List<String> lyricsHistory = new ArrayList<>();
        lyricsHistory.add("Lyrics: " + lyrics + " | Timestamp: " + System.currentTimeMillis());
        try {
            writeFile(lyricsHistoryFile, lyricsHistory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save lyrics history file: " + lyricsHistoryFile, e);
        }
    }

    public synchronized void saveAlbum(String artistNickName, String albumTitle, String releaseDate, List<String> songTitles) {
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
        String songFile = songDir + sanitizeFileName(songTitle) + ".txt";
        File file = new File(songFile);
        if (!file.exists()) {
            throw new IllegalStateException("Song file not found: " + songFile);
        }

        List<String> songData = readFile(songFile);
        boolean lyricsFound = false;
        for (int i = 0; i < songData.size(); i++) {
            if (songData.get(i).startsWith("Lyrics:")) {
                songData.set(i, "Lyrics: " + suggestedLyrics);
                lyricsFound = true;
                break;
            }
        }
        if (!lyricsFound) {
            throw new IllegalStateException("Lyrics not found in song file: " + songFile);
        }
        try {
            writeFile(songFile, songData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update song file: " + songFile, e);
        }

        String lyricsHistoryFile = songDir + sanitizeFileName(songTitle) + "-lyrics-history.txt";
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

        if (singlesDir.exists() && singlesDir.isDirectory()) {
            File[] songFolders = singlesDir.listFiles(File::isDirectory);
            if (songFolders != null) {
                for (File songFolder : songFolders) {
                    File songFile = new File(songFolder, songFolder.getName() + ".txt");
                    if (songFile.exists()) {
                        List<String> songData = readFile(songFile.getPath());
                        Song song = parseSongFromFile(songData, null);
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

        if (albumsDir.exists() && albumsDir.isDirectory()) {
            File[] albumFolders = albumsDir.listFiles(File::isDirectory);
            if (albumFolders != null) {
                for (File albumFolder : albumFolders) {
                    String albumFilePath = albumFolder.getPath() + "/album.txt";
                    File albumFile = new File(albumFilePath);
                    String albumTitle = albumFolder.getName();
                    String releaseDate = "Not set";
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
                                }
                            }
                        }
                    }

                    Album album = new Album(albumTitle, releaseDate, artist);
                    File[] songFolders = albumFolder.listFiles(File::isDirectory);
                    if (songFolders != null) {
                        for (File songFolder : songFolders) {
                            File songFile = new File(songFolder, songFolder.getName() + ".txt");
                            if (songFile.exists()) {
                                List<String> songData = readFile(songFile.getPath());
                                Song song = parseSongFromFile(songData, album);
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

    private Song parseSongFromFile(List<String> songData, Album album) {
        String title = null;
        String lyricsText = "";
        String releaseDate = "Not set";
        int likes = 0;
        int views = 0;
        String filePath = null;

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
                    case "Lyrics": lyricsText = value; break;
                    case "File Path": filePath = value; break;
                }
            }
        }

        if (title == null) {
            throw new IllegalStateException("Failed to parse song: Song title is missing in data: " + songData);
        }

        Song song = new Song(title, lyricsText, releaseDate);
        song.setLikes(likes);
        song.setViews(views);
        song.setFilePath(filePath);
        if (album != null) {
            song.setAlbum(album);
        }
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