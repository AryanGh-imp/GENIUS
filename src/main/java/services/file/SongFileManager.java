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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.FileUtil.*;

public class SongFileManager extends FileManager {

    private String getAlbumDir(String artistNickName, String albumTitle) {
        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeAlbumTitle = sanitizeFileName(albumTitle);
        return DATA_DIR + "artists/" + safeArtistNickName + "/albums/" + safeAlbumTitle + "/";
    }

    public String getSongDir(String artistNickName, String songTitle, String albumName) {
        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeSongTitle = sanitizeFileName(songTitle);
        return albumName != null && !albumName.isEmpty()
                ? getAlbumDir(artistNickName, albumName) + safeSongTitle + "/"
                : DATA_DIR + "artists/" + safeArtistNickName + "/singles/" + safeSongTitle + "/";
    }

    // TODO : In the future
    private List<String> parseArtistNickNames(List<String> songData) {
        return songData.stream()
                .filter(line -> line.startsWith("Artists: "))
                .findFirst()
                .map(line -> List.of(line.substring("Artists: ".length()).split(",")))
                .orElse(Collections.emptyList());
    }

    public synchronized void saveSongsAndAlbumsForArtist(Artist artist) {
        if (artist == null) throw new IllegalArgumentException("Artist cannot be null");
        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        ensureDataDirectoryExists(artistDir + "singles/");
        ensureDataDirectoryExists(artistDir + "albums/");

        // Get existing directories
        List<String> existingSingles = listDirectories(artistDir + "singles/");
        List<String> existingAlbums = listDirectories(artistDir + "albums/");

        List<String> currentSingles = artist.getSingles().stream().map(Song::getTitle).map(FileUtil::sanitizeFileName).toList();
        existingSingles.stream().filter(s -> !currentSingles.contains(s)).forEach(s -> deleteSong(artist.getNickName(), s, null));
        List<String> currentAlbums = artist.getAlbums().stream().map(Album::getTitle).map(FileUtil::sanitizeFileName).toList();
        existingAlbums.stream().filter(a -> !currentAlbums.contains(a)).forEach(a -> deleteAlbum(artist.getNickName(), a));

        // Save current singles and albums
        artist.getSingles().forEach(single -> saveSong(Collections.singletonList(artist.getNickName()), single.getTitle(), null, single.getLyrics(), single.getReleaseDate(), single.getLikes(), single.getViews(), single.getAlbumArtPath()));
        artist.getAlbums().forEach(album -> {
            saveAlbum(artist.getNickName(), album.getTitle(), album.getReleaseDate(), album.getSongs().stream().map(Song::getTitle).toList(), album.getAlbumArtPath());
            album.getSongs().forEach(song -> saveSong(Collections.singletonList(artist.getNickName()), song.getTitle(), album.getTitle(), song.getLyrics(), song.getReleaseDate(), song.getLikes(), song.getViews(), song.getAlbumArtPath()));
        });
    }

    private List<String> listDirectories(String path) {
        try (Stream<Path> stream = Files.list(Paths.get(path))) {
            return stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list directory: " + path, e);
        }
    }

    public String loadLyrics(String metaFilePath) {
        String lyricsFilePath = metaFilePath.replace(".txt", "_lyrics.txt");
        return Files.exists(Paths.get(lyricsFilePath)) ? String.join("\n", readFile(lyricsFilePath)) : null;
    }

    public synchronized void saveSong(List<String> artistNickNames, String songTitle, String albumName, String lyrics, String releaseDate, int likes, int views, String albumArtPath) {
        if (artistNickNames == null || artistNickNames.isEmpty()) throw new IllegalArgumentException("Artist nicknames list cannot be null or empty");
        if (songTitle == null || songTitle.isEmpty()) throw new IllegalArgumentException("Song title cannot be null or empty");
        if (lyrics == null) throw new IllegalArgumentException("Lyrics cannot be null");
        if (releaseDate == null || releaseDate.isEmpty()) throw new IllegalArgumentException("Release date cannot be null or empty");

        String artistNickName = artistNickNames.getFirst(); // Only the first artist's name is used.
        String songDir = getSongDir(artistNickName, songTitle, albumName);
        ensureDataDirectoryExists(songDir);

        String safeSongTitle = sanitizeFileName(songTitle);
        List<String> songData = new ArrayList<>();
        songData.add("Song Name: " + songTitle);
        songData.add("Artists: " + artistNickName);
        songData.add("Likes: " + likes);
        songData.add("Views: " + views);
        songData.add("Release Date: " + releaseDate);
        if (albumArtPath != null) songData.add("AlbumArtPath: " + albumArtPath);
        writeFile(songDir + safeSongTitle + ".txt", songData);
        writeFile(songDir + safeSongTitle + "_lyrics.txt", Collections.singletonList(lyrics));
    }

    public synchronized void saveAlbum(String artistNickName, String albumTitle, String releaseDate, List<String> songTitles, String albumArtPath) {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (albumTitle == null || albumTitle.isEmpty()) throw new IllegalArgumentException("Album title cannot be null or empty");
        if (releaseDate == null) throw new IllegalArgumentException("Release date cannot be null");

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        ensureDataDirectoryExists(albumDir);

        List<String> albumData = new ArrayList<>();
        albumData.add("Album Title: " + albumTitle);
        albumData.add("Release Date: " + releaseDate);
        albumData.add("Songs: " + (songTitles != null ? String.join(",", songTitles) : ""));
        if (albumArtPath != null && !albumArtPath.isEmpty()) albumData.add("AlbumArtPath: " + albumArtPath);
        writeFile(albumDir + "album.txt", albumData);
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

    public void loadSongsAndAlbumsForArtist(Artist artist) {
        if (artist == null) throw new IllegalArgumentException("Artist cannot be null");
        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        File artistDirFile = new File(artistDir);
        if (!artistDirFile.exists()) return;

        File singlesDir = new File(artistDir + "singles/");
        if (singlesDir.exists() && singlesDir.isDirectory()) {
            try (Stream<Path> songFolders = Files.list(singlesDir.toPath()).filter(Files::isDirectory)) {
                songFolders.forEach(songFolder -> {
                    try {
                        File songFile = new File(songFolder.toFile(), songFolder.getFileName().toString() + ".txt");
                        if (songFile.exists()) {
                            List<String> songData = readFile(songFile.getPath());
                            String lyrics = loadLyrics(songFile.getPath());
                            Song song = parseSongFromFile(songData, null, lyrics);
                            song.addArtist(artist);
                            artist.getSingles().add(song);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing single song folder " + songFolder + ": " + e.getMessage());
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to process singles directory: " + singlesDir, e);
            }
        }

        File albumsDir = new File(artistDir + "albums/");
        if (albumsDir.exists() && albumsDir.isDirectory()) {
            try (Stream<Path> albumFolders = Files.list(albumsDir.toPath()).filter(Files::isDirectory)) {
                albumFolders.forEach(albumFolder -> {
                    try {
                        String albumFilePath = albumFolder + "/album.txt";
                        File albumFile = new File(albumFilePath);
                        if (!albumFile.exists()) return;
                        String albumTitle = albumFolder.getFileName().toString();
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
                                File songFile = new File(songFolder.toFile(), songFolder.getFileName().toString() + ".txt");
                                if (songFile.exists()) {
                                    List<String> songData = readFile(songFile.getPath());
                                    String lyrics = loadLyrics(songFile.getPath());
                                    Song song = parseSongFromFile(songData, album, lyrics);
                                    song.addArtist(artist);
                                    album.addSong(song);
                                }
                            });
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to process songs in album: " + albumTitle, e);
                        }
                        artist.getAlbums().add(album);
                    } catch (Exception e) {
                        System.err.println("Error processing album folder " + albumFolder + ": " + e.getMessage());
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to process albums directory: " + albumsDir, e);
            }
        }
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
            artPath = data.stream().filter(line -> line.startsWith("AlbumArtPath: ")).findFirst().map(line -> line.substring("AlbumArtPath: ".length())).orElse(null);
        }

        FileUtil.deleteDirectory(dirFile);
        if (artPath != null && !artPath.isEmpty()) {
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
    }

    public synchronized void deleteSong(String artistNickName, String songTitle, String albumName) {
        deleteEntity(artistNickName, songTitle, albumName, false);
    }

    public synchronized void deleteAlbum(String artistNickName, String albumTitle) {
        deleteEntity(artistNickName, albumTitle, null, true);
    }

    public synchronized String saveAlbumArt(String artistNickName, String albumTitle, File imageFile) throws IOException {
        if (artistNickName == null || artistNickName.isEmpty()) throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        if (albumTitle == null || albumTitle.isEmpty()) throw new IllegalArgumentException("Album title cannot be null or empty");
        if (imageFile == null || !imageFile.exists()) throw new IllegalArgumentException("Image file cannot be null or does not exist");

        String albumDir = getAlbumDir(artistNickName, albumTitle);
        ensureDataDirectoryExists(albumDir);
        String fileExtension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String artFile = albumDir + "album_art" + fileExtension;
        Files.copy(imageFile.toPath(), Paths.get(artFile), StandardCopyOption.REPLACE_EXISTING);

        File albumFile = ensureAndGetFile(albumDir + "album.txt");
        List<String> albumData = readFile(albumFile.getPath());
        List<String> updatedData = albumData.stream()
                .map(line -> line.startsWith("AlbumArtPath: ") ? "AlbumArtPath: " + artFile : line)
                .collect(Collectors.toList());
        if (albumData.stream().noneMatch(line -> line.startsWith("AlbumArtPath: "))) updatedData.add("AlbumArtPath: " + artFile);
        readAndUpdateFile(albumFile.getPath(), updatedData);
        return artFile;
    }

    public Song parseSongFromFile(List<String> songData, Album album, String lyrics) {
        String title = extractField(songData, "Song Name: ");
        if (title == null) {
            throw new IllegalStateException("Failed to parse song: Song title is missing in data: " + songData);
        }

        String releaseDate = extractField(songData, "Release Date: ") != null ? extractField(songData, "Release Date: ") : "Not set";
        String likesStr = extractField(songData, "Likes: ");
        int likes = likesStr != null ? Integer.parseInt(likesStr) : 0;
        String viewsStr = extractField(songData, "Views: ");
        int views = viewsStr != null ? Integer.parseInt(viewsStr) : 0;
        String albumArtPath = extractField(songData, "AlbumArtPath: ");

        Song song = new Song(title, lyrics != null ? lyrics : "", releaseDate);
        song.setLikes(likes);
        song.setViews(views);
        if (album != null) song.setAlbum(album);
        if (albumArtPath != null) song.setAlbumArtPath(albumArtPath);
        return song;
    }
}




// TODO: This class should be further investigated for optimization in the future.