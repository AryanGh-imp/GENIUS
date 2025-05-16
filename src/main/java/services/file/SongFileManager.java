package services.file;

import models.account.Artist;
import models.music.Album;
import models.music.Comment;
import models.music.Song;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.FileUtil.*;

public class SongFileManager extends FileManager {
    private final Map<String, List<Song>> songCache = new HashMap<>();

    public String getAlbumDir(String artistNickName, String albumTitle) {
        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeAlbumTitle = sanitizeFileName(albumTitle);
        return DATA_DIR + "artists/" + safeArtistNickName + "/albums/" + safeAlbumTitle + "/";
    }

    public String getSongDir(String artistNickname, String songTitle, String albumName) {
        String safeArtistNickName = sanitizeFileName(artistNickname);
        String safeSongTitle = sanitizeFileName(songTitle);
        String basePath = DATA_DIR + "artists/" + safeArtistNickName + "/";
        if (albumName == null || albumName.equals("Single") || albumName.trim().isEmpty()) {
            return basePath + "singles/" + safeSongTitle + "/";
        } else {
            return basePath + "albums/" + sanitizeFileName(albumName) + "/" + safeSongTitle + "/";
        }
    }

    public synchronized String saveSingleSongArt(String artistNickName, String songTitle, File imageFile) throws IOException {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (songTitle == null || songTitle.isEmpty()) throw new IllegalArgumentException("Song title cannot be null or empty");
        if (imageFile == null || !imageFile.exists()) throw new IllegalArgumentException("Image file cannot be null or does not exist");

        String songDir = getSongDir(artistNickName, songTitle, null);
        ensureDataDirectoryExists(songDir);

        String fileExtension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String artFile = songDir + "song_art" + fileExtension;

        try {
            Files.copy(imageFile.toPath(), Paths.get(artFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to copy image file to: " + artFile + " - " + e.getMessage());
            throw new IOException("Unable to save song art: " + e.getMessage(), e);
        }

        File songDirFile = new File(songDir);
        File[] existingArtFiles = songDirFile.listFiles((dir, name) -> name.startsWith("song_art.") && !name.equals("song_art" + fileExtension));
        if (existingArtFiles != null) {
            for (File oldArtFile : existingArtFiles) {
                if (oldArtFile.exists()) oldArtFile.delete();
            }
        }

        System.out.println("Saved song art to: " + artFile);
        return artFile;
    }

    public synchronized void saveSongsAndAlbumsForArtist(Artist artist) {
        if (artist == null) throw new IllegalArgumentException("Artist cannot be null");
        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        ensureDataDirectoryExists(artistDir + "singles/");
        ensureDataDirectoryExists(artistDir + "albums/");

        List<String> existingSingles = listDirectories(artistDir + "singles/");
        List<String> existingAlbums = listDirectories(artistDir + "albums/");
        List<String> currentSingles = artist.getSingles().stream().map(Song::getTitle).map(FileUtil::sanitizeFileName).toList();
        List<String> currentAlbums = artist.getAlbums().stream().map(Album::getTitle).map(FileUtil::sanitizeFileName).toList();

        existingSingles.stream()
                .filter(s -> !currentSingles.contains(s))
                .forEach(s -> {
                    String songDirPath = getSongDir(artist.getNickName(), s, null);
                    File songDir = new File(songDirPath);
                    if (songDir.exists()) {
                        File metaFile = new File(songDirPath + s + ".txt");
                        if (metaFile.exists()) {
                            List<String> metaData = readFile(metaFile.getPath());
                            String artPath = metaData.stream()
                                    .filter(line -> line.startsWith("SongArtPath: ") || line.startsWith("AlbumArtPath: "))
                                    .findFirst()
                                    .map(line -> line.substring(line.indexOf(": ") + 2))
                                    .orElse(null);
                            deleteSong(artist.getNickName(), s, null);
                            if (artPath != null) {
                                File artFile = new File(artPath);
                                if (artFile.exists()) artFile.delete();
                            }
                        }
                    }
                });

        existingAlbums.stream()
                .filter(a -> !currentAlbums.contains(a))
                .forEach(a -> {
                    File albumDir = new File(getAlbumDir(artist.getNickName(), a));
                    if (albumDir.exists()) deleteAlbum(artist.getNickName(), a);
                });

        for (Song single : artist.getSingles()) {
            String songDir = getSongDir(artist.getNickName(), single.getTitle(), null);
            File songFile = new File(songDir + single.getTitle() + ".txt");
            ensureDataDirectoryExists(songDir);

            if (songFile.exists()) {
                List<String> existingData = readFile(songFile.getPath());
                String existingLikesStr = extractField(existingData, "Likes: ") != null ? extractField(existingData, "Likes: ") : "0";
                String existingViewsStr = extractField(existingData, "Views: ") != null ? extractField(existingData, "Views: ") : "0";
                int existingLikes = Integer.parseInt(existingLikesStr);
                int existingViews = Integer.parseInt(existingViewsStr);
                single.setLikes(existingLikes);
                single.setViews(existingViews);
            }

            if (!songFile.exists() || hasSongChanged(songFile, single)) {
                String songArtPath = single.getAlbumArtPath();
                if (songArtPath == null || !new File(songArtPath).exists()) {
                    File songDirFile = new File(songDir);
                    File[] artFiles = songDirFile.listFiles((dir, name) -> name.startsWith("song_art."));
                    if (artFiles != null && artFiles.length > 0) {
                        songArtPath = artFiles[0].getAbsolutePath();
                        single.setAlbumArtPath(songArtPath);
                    }
                }
                saveSong(Collections.singletonList(artist.getNickName()), single.getTitle(), null, single.getLyrics(), single.getReleaseDate(), single.getLikes(), single.getViews(), songArtPath);
                System.out.println("Saved single: " + single.getTitle() + ", Likes: " + single.getLikes() + ", Views: " + single.getViews());
            } else {
                System.out.println("No changes detected for single: " + single.getTitle() + ", skipping save.");
            }
        }

        for (Album album : artist.getAlbums()) {
            String albumDir = getAlbumDir(artist.getNickName(), album.getTitle());
            File albumFile = new File(albumDir + "album.txt");
            ensureDataDirectoryExists(albumDir);

            if (!albumFile.exists() || hasAlbumChanged(albumFile, album)) {
                List<String> songTitles = album.getSongs().stream().map(Song::getTitle).toList();
                saveAlbum(artist.getNickName(), album.getTitle(), album.getReleaseDate(), songTitles, album.getAlbumArtPath());
            } else {
                System.out.println("No changes detected for album: " + album.getTitle() + ", skipping save.");
            }

            for (Song song : album.getSongs()) {
                String songDir = getSongDir(artist.getNickName(), song.getTitle(), album.getTitle());
                File songFile = new File(songDir + song.getTitle() + ".txt");
                ensureDataDirectoryExists(songDir);

                if (songFile.exists()) {
                    List<String> existingData = readFile(songFile.getPath());
                    String existingLikesStr = extractField(existingData, "Likes: ") != null ? extractField(existingData, "Likes: ") : "0";
                    String existingViewsStr = extractField(existingData, "Views: ") != null ? extractField(existingData, "Views: ") : "0";
                    int existingLikes = Integer.parseInt(existingLikesStr);
                    int existingViews = Integer.parseInt(existingViewsStr);
                    song.setLikes(existingLikes);
                    song.setViews(existingViews);
                }

                if (!songFile.exists() || hasSongChanged(songFile, song)) {
                    String songArtPath = song.getAlbumArtPath();
                    if (songArtPath == null || !new File(songArtPath).exists()) {
                        if (album.getAlbumArtPath() != null && new File(album.getAlbumArtPath()).exists()) {
                            String fileExtension = album.getAlbumArtPath().substring(album.getAlbumArtPath().lastIndexOf("."));
                            String newArtFile = songDir + "song_art" + fileExtension;
                            try {
                                Files.copy(Paths.get(album.getAlbumArtPath()), Paths.get(newArtFile), StandardCopyOption.REPLACE_EXISTING);
                                songArtPath = newArtFile;
                                song.setAlbumArtPath(songArtPath);
                            } catch (IOException e) {
                                System.err.println("Failed to copy album art to song directory: " + newArtFile + " - " + e.getMessage());
                            }
                        }
                    }
                    saveSong(Collections.singletonList(artist.getNickName()), song.getTitle(), album.getTitle(), song.getLyrics(), song.getReleaseDate(), song.getLikes(), song.getViews(), songArtPath);
                    System.out.println("Saved song: " + song.getTitle() + " in album: " + album.getTitle() + ", Likes: " + song.getLikes() + ", Views: " + song.getViews());
                } else {
                    System.out.println("No changes detected for song: " + song.getTitle() + " in album: " + album.getTitle() + ", skipping save.");
                }
            }
        }

        songCache.put(safeNickName, new ArrayList<>(artist.getSingles()));
        artist.getAlbums().forEach(album -> songCache.computeIfAbsent(safeNickName, k -> new ArrayList<>()).addAll(album.getSongs()));
    }

    private boolean hasSongChanged(File songFile, Song song) {
        if (!songFile.exists()) return true;
        List<String> existingData = readFile(songFile.getPath());
        String existingLyrics = loadLyrics(songFile.getPath());
        String existingTitle = extractField(existingData, "Song Name: ");
        String existingReleaseDate = extractField(existingData, "Release Date: ");
        String existingLikesStr = extractField(existingData, "Likes: ") != null ? extractField(existingData, "Likes: ") : "0";
        String existingViewsStr = extractField(existingData, "Views: ") != null ? extractField(existingData, "Views: ") : "0";
        String existingArtPath = extractField(existingData, "SongArtPath: ") != null ? extractField(existingData, "SongArtPath: ") : extractField(existingData, "AlbumArtPath: ");

        int existingLikes = Integer.parseInt(existingLikesStr);
        int existingViews = Integer.parseInt(existingViewsStr);

        song.setLikes(existingLikes);
        song.setViews(existingViews);

        boolean changed = !song.getTitle().equals(existingTitle) ||
                !song.getLyrics().equals(existingLyrics != null ? existingLyrics : "") ||
                !song.getReleaseDate().equals(existingReleaseDate) ||
                (song.getLikes() != existingLikes) ||
                (song.getViews() != existingViews) ||
                (song.getAlbumArtPath() != null && !song.getAlbumArtPath().equals(existingArtPath));

        if (changed) {
            System.out.println("Changes detected for song: " + song.getTitle() + " - Old Likes: " + existingLikes + ", New Likes: " + song.getLikes() +
                    ", Old Views: " + existingViews + ", New Views: " + song.getViews());
        } else {
            System.out.println("No changes detected for song: " + song.getTitle() + " - Existing Likes: " + existingLikes + ", Existing Views: " + existingViews);
        }
        return changed;
    }

    private boolean hasAlbumChanged(File albumFile, Album album) {
        if (!albumFile.exists()) return true;
        List<String> existingData = readFile(albumFile.getPath());
        String existingTitle = extractField(existingData, "Album Title: ");
        String existingReleaseDate = extractField(existingData, "Release Date: ");
        String existingSongs = extractField(existingData, "Songs: ");
        String existingArtPath = extractField(existingData, "AlbumArtPath: ");

        List<String> existingSongTitles = existingSongs != null ? List.of(existingSongs.split(",")) : new ArrayList<>();
        List<String> currentSongTitles = album.getSongs().stream().map(Song::getTitle).toList();

        return !album.getTitle().equals(existingTitle) ||
                !album.getReleaseDate().equals(existingReleaseDate) ||
                !currentSongTitles.equals(existingSongTitles) ||
                (album.getAlbumArtPath() != null && !album.getAlbumArtPath().equals(existingArtPath));
    }

    public List<String> listDirectories(String path) {
        try (Stream<Path> stream = Files.list(Paths.get(path))) {
            return stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list directory: " + path, e);
        }
    }

    public String loadLyrics(String metaFilePath) {
        String lyricsFilePath = metaFilePath.replace(".txt", "_lyrics.txt");
        System.out.println("Checking lyrics file: " + lyricsFilePath);
        if (Files.exists(Paths.get(lyricsFilePath))) {
            List<String> lyricsLines = readFile(lyricsFilePath);
            System.out.println("Lines read from lyrics file: " + lyricsLines);
            String lyrics = String.join("\n", lyricsLines).trim();
            return lyrics.isEmpty() ? null : lyrics;
        }
        System.out.println("Lyrics file does not exist: " + lyricsFilePath);
        return null;
    }

    public synchronized void saveSong(List<String> artistNickNames, String songTitle, String albumName, String lyrics, String releaseDate, int likes, int views, String songArtPath) {
        if (artistNickNames == null || artistNickNames.isEmpty()) throw new IllegalArgumentException("Artist nicknames list cannot be null or empty");
        if (songTitle == null || songTitle.isEmpty()) throw new IllegalArgumentException("Song title cannot be null or empty");
        if (lyrics == null) throw new IllegalArgumentException("Lyrics cannot be null");
        if (releaseDate == null || releaseDate.isEmpty()) throw new IllegalArgumentException("Release date cannot be null or empty");

        String artistNickName = artistNickNames.getFirst();
        String songDir = getSongDir(artistNickName, songTitle, albumName);
        ensureDataDirectoryExists(songDir);

        String alternatePath = (albumName != null && !albumName.isEmpty())
                ? FileUtil.DATA_DIR + "artists/" + artistNickName + "/singles/" + songTitle + "/"
                : FileUtil.DATA_DIR + "artists/" + artistNickName + "/albums/";
        File alternateDir = new File(alternatePath + songTitle + "/" + songTitle + ".txt");
        if (alternateDir.exists()) {
            System.err.println("Warning: Song found in alternate path, deleting: " + alternateDir.getPath());
            FileUtil.deleteDirectory(alternateDir.getParentFile());
        }

        String safeSongTitle = sanitizeFileName(songTitle);

        File songDirFile = new File(songDir);
        File[] existingFiles = songDirFile.listFiles((dir, name) ->
                (name.endsWith(".txt") || name.endsWith("_lyrics.txt")) &&
                        !name.equals(safeSongTitle + ".txt") &&
                        !name.equals(safeSongTitle + "_lyrics.txt") &&
                        !name.endsWith("-comments.txt"));
        if (existingFiles != null) {
            for (File oldFile : existingFiles) {
                try {
                    Files.deleteIfExists(oldFile.toPath());
                    System.out.println("Deleted old file: " + oldFile.getPath());
                } catch (IOException e) {
                    System.err.println("Failed to delete old file: " + oldFile.getPath() + " - " + e.getMessage());
                }
            }
        }

        List<String> songData = new ArrayList<>();
        songData.add("Song Name: " + songTitle);
        songData.add("Artists: " + String.join(",", artistNickNames));
        songData.add("Likes: " + likes);
        songData.add("Views: " + views);
        songData.add("Release Date: " + releaseDate);
        if (songArtPath != null && !songArtPath.isEmpty()) {
            songData.add("SongArtPath: " + songArtPath);
        }

        System.out.println("Saving song with Likes: " + likes + ", Views: " + views + ", ArtPath: " + songArtPath + ", Path: " + songDir);
        writeFile(songDir + safeSongTitle + ".txt", songData);
        writeFile(songDir + safeSongTitle + "_lyrics.txt", Collections.singletonList(lyrics));

        if (albumName != null && !albumName.isEmpty()) {
            String albumDir = getAlbumDir(artistNickName, albumName);
            String albumFilePath = albumDir + "album.txt";
            File albumFile = new File(albumFilePath);
            ensureDataDirectoryExists(albumDir);

            List<String> albumData = albumFile.exists() ? readFile(albumFilePath) : new ArrayList<>();
            List<String> updatedAlbumData = new ArrayList<>();
            List<String> existingSongs = new ArrayList<>();
            boolean songsLineUpdated = false;

            for (String line : albumData) {
                if (line.trim().startsWith("Songs: ")) {
                    if (!songsLineUpdated) {
                        String songsStr = line.substring("Songs: ".length()).trim();
                        if (!songsStr.isEmpty()) existingSongs.addAll(List.of(songsStr.split(",")));
                        if (!existingSongs.contains(songTitle)) {
                            existingSongs.add(songTitle);
                        }
                        updatedAlbumData.add("Songs: " + existingSongs.stream().distinct().collect(Collectors.joining(",")));
                        songsLineUpdated = true;
                    }
                } else {
                    updatedAlbumData.add(line);
                }
            }

            if (!songsLineUpdated) {
                updatedAlbumData.add("Songs: " + songTitle);
            }

            writeFile(albumFilePath, updatedAlbumData);
            System.out.println("Updated album.txt with songs: " + String.join(",", existingSongs));
        }

        List<Song> songs = songCache.getOrDefault(artistNickName, new ArrayList<>());
        songs.add(new Song(songTitle, lyrics, releaseDate));
        songCache.put(artistNickName, songs);
    }

    public synchronized void saveAlbum(String artistNickName, String albumTitle, String releaseDate, List<String> songTitles, String albumArtPath) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (albumTitle == null || albumTitle.isEmpty()) throw new IllegalArgumentException("Album title cannot be null or empty");
        if (releaseDate == null) throw new IllegalArgumentException("Release date cannot be null");

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        ensureDataDirectoryExists(albumDir);

        String albumFilePath = albumDir + "album.txt";
        File albumFile = new File(albumFilePath);

        List<String> updatedData = new ArrayList<>();
        updatedData.add("Album Title: " + albumTitle);
        updatedData.add("Release Date: " + releaseDate);
        if (songTitles != null && !songTitles.isEmpty()) {
            updatedData.add("Songs: " + songTitles.stream().distinct().collect(Collectors.joining(",")));
        } else {
            updatedData.add("Songs: ");
        }
        if (albumArtPath != null && !albumArtPath.isEmpty()) {
            updatedData.add("AlbumArtPath: " + albumArtPath);
        }

        try {
            File parentDir = albumFile.getParentFile();
            if (!parentDir.exists() || !parentDir.canWrite()) {
                ensureDataDirectoryExists(parentDir.getAbsolutePath());
                if (!parentDir.canWrite()) throw new IOException("Cannot write to directory: " + parentDir.getAbsolutePath());
            }
            if (!albumFile.exists() && !albumFile.createNewFile()) throw new IOException("Failed to create album file: " + albumFilePath);
            writeFile(albumFilePath, updatedData);
            System.out.println("Successfully created/updated album file: " + albumFilePath);
        } catch (IOException e) {
            System.err.println("Error creating album file: " + albumFilePath + " - " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Failed to create album file: " + albumFilePath, e);
        }
    }

    public synchronized String saveAlbumArt(String artistNickName, String albumTitle, File imageFile) throws IOException {
        return saveAlbumArt(artistNickName, albumTitle, imageFile, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    public synchronized String saveAlbumArt(String artistNickName, String albumTitle, File imageFile, String releaseDate) throws IOException {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (albumTitle == null || albumTitle.isEmpty()) throw new IllegalArgumentException("Album title cannot be null or empty");
        if (imageFile == null || !imageFile.exists()) throw new IllegalArgumentException("Image file cannot be null or does not exist");
        if (releaseDate == null) throw new IllegalArgumentException("Release date cannot be null");

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        ensureDataDirectoryExists(albumDir);

        File albumDirFile = new File(albumDir);
        File[] existingArtFiles = albumDirFile.listFiles((dir, name) -> name.startsWith("album_art."));
        if (existingArtFiles != null) {
            for (File oldArtFile : existingArtFiles) {
                if (oldArtFile.exists()) oldArtFile.delete();
            }
        }

        String fileExtension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String artFile = albumDir + "album_art" + fileExtension;
        try {
            Files.copy(imageFile.toPath(), Paths.get(artFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to copy album art to: " + artFile + " - " + e.getMessage());
            throw new IOException("Unable to save album art: " + e.getMessage(), e);
        }

        File albumFile = new File(albumDir + "album.txt");
        List<String> albumData = albumFile.exists() ? readFile(albumFile.getPath()) : new ArrayList<>();
        List<String> existingSongs = new ArrayList<>();

        for (String line : albumData) {
            if (line.trim().startsWith("Songs: ")) {
                String songsStr = line.substring("Songs: ".length()).trim();
                if (!songsStr.isEmpty()) existingSongs.addAll(List.of(songsStr.split(",")));
            }
        }

        List<String> updatedData = new ArrayList<>();
        updatedData.add("Album Title: " + albumTitle);
        updatedData.add("Release Date: " + releaseDate);
        updatedData.add("Songs: " + existingSongs.stream().distinct().collect(Collectors.joining(",")));
        updatedData.add("AlbumArtPath: " + artFile);

        writeFile(albumFile.getPath(), updatedData);
        System.out.println("Saved album art to: " + artFile);
        return artFile;
    }

    public synchronized void addComment(String artistNickName, String songTitle, String albumName, String commentText, String user) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (songTitle == null || songTitle.isEmpty()) throw new IllegalArgumentException("Song title cannot be null or empty");
        if (commentText == null || commentText.isEmpty()) throw new IllegalArgumentException("Comment text cannot be null or empty");
        if (user == null || user.isEmpty()) throw new IllegalArgumentException("User cannot be null or empty");

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        ensureDataDirectoryExists(songDir);

        String commentsFile = songDir + sanitizeFileName(songTitle) + "-comments.txt";
        List<Comment> comments = Files.exists(Paths.get(commentsFile)) ? loadComments(artistNickName, songTitle, albumName) : new ArrayList<>();
        comments.add(new Comment(user, commentText));
        writeFile(commentsFile, comments.stream().map(Comment::toString).collect(Collectors.toList()));
    }

    public synchronized void addAlbumComment(String artistNickName, String albumName, String commentText, String user) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (albumName == null || albumName.isEmpty()) throw new IllegalArgumentException("Album name cannot be null or empty");
        if (commentText == null || commentText.isEmpty()) throw new IllegalArgumentException("Comment text cannot be null or empty");
        if (user == null || user.isEmpty()) throw new IllegalArgumentException("User cannot be null or empty");

        String albumDir = getAlbumDir(artistNickName, albumName);
        ensureDataDirectoryExists(albumDir);

        String commentsFile = albumDir + sanitizeFileName(albumName) + "-album-comments.txt";
        List<Comment> comments = Files.exists(Paths.get(commentsFile)) ? loadAlbumComments(artistNickName, albumName) : new ArrayList<>();
        comments.add(new Comment(user, commentText));
        writeFile(commentsFile, comments.stream().map(Comment::toString).collect(Collectors.toList()));
    }

    public synchronized List<Comment> loadComments(String artistNickName, String songTitle, String albumName) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (songTitle == null || songTitle.isEmpty()) throw new IllegalArgumentException("Song title cannot be null or empty");

        String songDir = getSongDir(artistNickName, songTitle, albumName);
        String commentsFile = songDir + sanitizeFileName(songTitle) + "-comments.txt";
        return Files.exists(Paths.get(commentsFile))
                ? readFile(commentsFile).stream()
                .map(line -> {
                    String[] parts = line.split(" \\| ");
                    if (parts.length != 3) throw new IllegalStateException("Invalid comment format in file: " + commentsFile + ", line: " + line);
                    return new Comment(parts[0].substring("User: ".length()), parts[2].substring("Comment: ".length()), LocalDateTime.parse(parts[1].substring("Time: ".length()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                })
                .collect(Collectors.toList())
                : new ArrayList<>();
    }

    public synchronized List<Comment> loadAlbumComments(String artistNickName, String albumName) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (albumName == null || albumName.isEmpty()) throw new IllegalArgumentException("Album name cannot be null or empty");

        String albumDir = getAlbumDir(artistNickName, albumName);
        String commentsFile = albumDir + sanitizeFileName(albumName) + "-album-comments.txt";
        return Files.exists(Paths.get(commentsFile))
                ? readFile(commentsFile).stream()
                .map(line -> {
                    String[] parts = line.split(" \\| ");
                    if (parts.length != 3) throw new IllegalStateException("Invalid comment format in file: " + commentsFile + ", line: " + line);
                    return new Comment(parts[0].substring("User: ".length()), parts[2].substring("Comment: ".length()), LocalDateTime.parse(parts[1].substring("Time: ".length()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                })
                .collect(Collectors.toList())
                : new ArrayList<>();
    }

    public void loadSongsAndAlbumsForArtist(Artist artist, ArtistFileManager artistFileManager) {
        if (artist == null) throw new IllegalArgumentException("Artist cannot be null");
        if (artistFileManager == null) throw new IllegalArgumentException("ArtistFileManager cannot be null");

        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        File artistDirFile = new File(artistDir);
        if (!artistDirFile.exists()) {
            System.out.println("Artist directory does not exist: " + artistDir);
            return;
        }

        System.out.println("Loading songs and albums for artist: " + safeNickName);

        List<Song> loadedSingles = new ArrayList<>();
        List<Album> loadedAlbums = new ArrayList<>();

        File singlesDir = new File(artistDir + "singles/");
        if (singlesDir.exists() && singlesDir.isDirectory()) {
            try (Stream<Path> songFolders = Files.list(singlesDir.toPath()).filter(Files::isDirectory)) {
                songFolders.forEach(songFolder -> {
                    try {
                        String songTitle = songFolder.getFileName().toString();
                        File songFile = new File(songFolder.toFile(), songTitle + ".txt");
                        if (songFile.exists()) {
                            List<String> songData = readFile(songFile.getPath());
                            System.out.println("Raw song data for single " + songTitle + ": " + songData);
                            String lyrics = loadLyrics(songFile.getPath());
                            Song song = parseSongFromFile(songData, null, lyrics, artist);
                            System.out.println("Loaded single: " + song.getTitle() + ", Likes: " + song.getLikes() + ", Views: " + song.getViews());
                            loadedSingles.add(song);
                        } else {
                            System.out.println("Meta file not found for single: " + songTitle);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing single song folder " + songFolder + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                System.err.println("Failed to process singles directory: " + singlesDir + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        File albumsDir = new File(artistDir + "albums/");
        if (albumsDir.exists() && albumsDir.isDirectory()) {
            try (Stream<Path> albumFolders = Files.list(albumsDir.toPath()).filter(Files::isDirectory)) {
                albumFolders.forEach(albumFolder -> {
                    try {
                        String albumTitle = albumFolder.getFileName().toString();
                        String albumFilePath = albumFolder + "/album.txt";
                        File albumFile = new File(albumFilePath);
                        if (!albumFile.exists()) {
                            System.out.println("Album file not found: " + albumFilePath);
                            return;
                        }
                        String releaseDate = "Not set";
                        String albumArtPath = null;

                        List<String> albumData = readFile(albumFilePath);
                        for (String line : albumData) {
                            int index = line.indexOf(": ");
                            if (index != -1) {
                                String key = line.substring(0, index);
                                String value = line.substring(index + 2);
                                if (key.equals("Release Date")) releaseDate = value;
                                else if (key.equals("AlbumArtPath")) albumArtPath = value;
                            }
                        }

                        Album album = new Album(albumTitle, releaseDate, artist);
                        if (albumArtPath != null) album.setAlbumArtPath(albumArtPath);
                        try (Stream<Path> songFolders = Files.list(albumFolder).filter(Files::isDirectory)) {
                            songFolders.forEach(songFolder -> {
                                String songTitle = songFolder.getFileName().toString();
                                File songFile = new File(songFolder.toFile(), songTitle + ".txt");
                                if (songFile.exists()) {
                                    List<String> songData = readFile(songFile.getPath());
                                    System.out.println("Raw song data for song " + songTitle + " in album " + albumTitle + ": " + songData);
                                    String lyrics = loadLyrics(songFile.getPath());
                                    Song song = parseSongFromFile(songData, album, lyrics, artist);
                                    System.out.println("Loaded song: " + song.getTitle() + " in album: " + albumTitle + ", Likes: " + song.getLikes() + ", Views: " + song.getViews());
                                    album.addSong(song);
                                } else {
                                    System.out.println("Meta file not found for song: " + songTitle + " in album: " + albumTitle);
                                }
                            });
                        }
                        loadedAlbums.add(album);
                    } catch (Exception e) {
                        System.err.println("Error processing album folder " + albumFolder + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                System.err.println("Failed to process albums directory: " + albumsDir + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        artist.clearSingles();
        loadedSingles.forEach(artist::addSingle);
        artist.clearAlbums();
        loadedAlbums.forEach(artist::addAlbum);

        System.out.println("Total singles loaded: " + loadedSingles.size());
        System.out.println("Total albums loaded: " + loadedAlbums.size());
        songCache.put(safeNickName, new ArrayList<>(loadedSingles));
        loadedAlbums.forEach(album -> songCache.computeIfAbsent(safeNickName, k -> new ArrayList<>()).addAll(album.getSongs()));
    }

    private void deleteEntity(String artistNickName, String entityName, String albumName, boolean isAlbum) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (entityName == null || entityName.isEmpty()) throw new IllegalArgumentException(isAlbum ? "Album title" : "Song title" + " cannot be null or empty");

        String dir = isAlbum ? getAlbumDir(artistNickName, entityName) : getSongDir(artistNickName, entityName, albumName);
        File dirFile = new File(dir);
        if (!dirFile.exists()) throw new IllegalStateException((isAlbum ? "Album" : "Song") + " directory not found: " + dir);

        String artPath = null;
        File metaFile = new File(dir + sanitizeFileName(entityName) + ".txt");
        if (metaFile.exists()) {
            List<String> data = readFile(metaFile.getPath());
            artPath = data.stream()
                    .filter(line -> line.startsWith("AlbumArtPath: ") || line.startsWith("SongArtPath: "))
                    .findFirst()
                    .map(line -> line.substring(line.indexOf(": ") + 2))
                    .orElse(null);
        }

        if (isAlbum) {
            String commentsFile = dir + sanitizeFileName(entityName) + "-album-comments.txt";
            File commentsFileObj = new File(commentsFile);
            if (commentsFileObj.exists()) {
                try {
                    Files.delete(commentsFileObj.toPath());
                    System.out.println("Deleted album comments file: " + commentsFile);
                } catch (IOException e) {
                    System.err.println("Failed to delete album comments file: " + commentsFile + " - " + e.getMessage());
                }
            }
        }

        if (FileUtil.deleteDirectory(dirFile) && artPath != null && !artPath.isEmpty()) {
            File artFile = new File(artPath);
            if (artFile.exists()) artFile.delete();
        }

        if (!isAlbum && albumName != null && !albumName.isEmpty()) {
            File albumFile = new File(getAlbumDir(artistNickName, albumName) + "album.txt");
            if (albumFile.exists()) {
                List<String> albumData = readFile(albumFile.getPath());
                List<String> updatedData = albumData.stream()
                        .map(line -> line.startsWith("Songs: ") && !line.equals("Songs: ")
                                ? "Songs: " + String.join(",", Stream.of(line.substring("Songs: ".length()).split(",")).filter(t -> !t.equals(entityName)).toList())
                                : line)
                        .collect(Collectors.toList());
                readAndUpdateFile(albumFile.getPath(), updatedData);
            }
        }

        String artistSafeNickName = sanitizeFileName(artistNickName);
        songCache.remove(artistSafeNickName);
    }

    public synchronized void deleteSong(String artistNickName, String songTitle, String albumName) {
        deleteEntity(artistNickName, songTitle, albumName, false);
    }

    public synchronized void deleteAlbum(String artistNickName, String albumTitle) {
        deleteEntity(artistNickName, albumTitle, null, true);
    }

    public Song parseSongFromFile(List<String> songData, Album album, String lyrics, Artist artist) {
        if (songData == null) throw new IllegalArgumentException("Song data cannot be null");
        String title = extractField(songData, "Song Name: ");
        if (title == null) throw new IllegalStateException("Failed to parse song: Song title is missing in data: " + songData);

        String releaseDate = extractField(songData, "Release Date: ");
        String likesStr = extractField(songData, "Likes: ");
        String viewsStr = extractField(songData, "Views: ");
        String songArtPath = extractField(songData, "SongArtPath: ");
        String albumArtPath = extractField(songData, "AlbumArtPath: ");

        int likes = likesStr != null ? Integer.parseInt(likesStr) : 0;
        int views = viewsStr != null ? Integer.parseInt(viewsStr) : 0;

        System.out.println("Parsing song - Title: " + title + ", ReleaseDate: " + releaseDate + ", Likes: " + likes + ", Views: " + views +
                ", SongArtPath: " + songArtPath + ", AlbumArtPath: " + albumArtPath);

        Song song = new Song(title, lyrics != null ? lyrics : "", releaseDate != null ? releaseDate : "Not set");
        song.addArtist(artist);
        song.setLikes(likes);
        song.setViews(views);
        if (album != null) song.setAlbum(album);
        song.setAlbumArtPath(songArtPath != null ? songArtPath : (albumArtPath));
        return song;
    }

    public void clearCache() {
        songCache.clear();
    }
}