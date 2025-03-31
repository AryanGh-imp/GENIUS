package utils;

import javafx.scene.control.Alert;
import models.Artist;
import models.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static utils.AlertUtil.showError;

public class FileUtil {
    private static final String DATA_DIR = "DATA/";

    // Check and create folder if it does not exist
    private static void ensureDataDirectoryExists(String DIR) {
        try {
            Files.createDirectories(Paths.get(DIR));  // Ensure directory exists
        } catch (IOException e) {
            System.err.println("Error creating directory: " + e.getMessage());
            showError("Failed to create directory: " + DIR);
        }
    }

    public static String sanitizeFileName(String name) {
        return name != null ? name.replaceAll("[^a-zA-Z0-9_-]", "_") : ""; // Only letters, numbers, _ are allowed.
    }


    // Checking for duplicate emails or nickname
    public static synchronized boolean isEmailOrNickNameTaken(String email, String nickName) {
        String safeNickName = sanitizeFileName(nickName);
        ensureDataDirectoryExists(DATA_DIR + "users/" + safeNickName);
        ensureDataDirectoryExists(DATA_DIR + "artists/" + safeNickName);
        String userFileName = safeNickName + "-" + email + ".txt";
        File userFile = new File(DATA_DIR + "users/" + safeNickName, userFileName);
        File artistFile = new File(DATA_DIR + "artists/" + safeNickName, userFileName);
        return userFile.exists() || artistFile.exists();
    }

    // Save user information to file
    public static synchronized void saveUser(String email, String nickName, String password) {
        String safeNickName = sanitizeFileName(nickName);
        if (isEmailOrNickNameTaken(email, safeNickName)) {
            showError("This email or nickname is already registered. Please try another one.");
            return;  // If the email is a duplicate, the data will not be saved.
        }

        ensureDataDirectoryExists(DATA_DIR);  // Ensure the folder exists

        String USER_DIR = DATA_DIR + "users/" + safeNickName + "/";

        ensureDataDirectoryExists(USER_DIR);

        File accFile = new File(USER_DIR, safeNickName + "-" + email + ".txt");
        List<String> userData = Arrays.asList(
                "Email: " + email,
                "Nickname: " + safeNickName,
                "Password: " + password);

            // Save following artists
        try {
            Files.write(accFile.toPath(), userData, StandardOpenOption.CREATE);
            Files.write(Paths.get(USER_DIR + "followings.txt"),
                    Collections.singletonList("Following: (None)"), StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
            showError("Failed to save user data!");
        }
    }

    public static synchronized void saveArtist(String email, String nickName, String password, boolean approved) {
        String safeNickName = sanitizeFileName(nickName);
        if (isEmailOrNickNameTaken(email, safeNickName)) {
            return;
        }
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        ensureDataDirectoryExists(artistDir);
        String infoFile = artistDir + safeNickName + "-" + email + ".txt";
        List<String> artistData = Arrays.asList(
                "Email: " + email,
                "Nickname: " + safeNickName,
                "Password: " + password,
                "Approved: " + approved);
        try {
            Files.write(Paths.get(infoFile), artistData, StandardOpenOption.CREATE);
            String followersFile = artistDir + "followers.txt";
            if (!new File(followersFile).exists()) {
                Files.write(Paths.get(followersFile), Collections.singletonList("Followers: (None)"), StandardOpenOption.CREATE);
            }
            String singlesDir = artistDir + "singles/";
            ensureDataDirectoryExists(singlesDir);
            String albumsDir = artistDir + "albums/";
            ensureDataDirectoryExists(albumsDir);
        } catch (IOException e) {
            System.err.println("Error saving artist data: " + e.getMessage());
            showError("Failed to save artist data!");
        }
    }

    // Save a song in singles or album folder
    public static synchronized void saveSong(String artistNickName, String songName, String albumName, String songDetails) {
        String safeArtistNickName = sanitizeFileName(artistNickName);
        String artistDir = DATA_DIR + "artists/" + safeArtistNickName + "/";
        if (!new File(artistDir).exists()) {
            showError("Artist does not exist!");
            return;
        }
        String safeSongName = sanitizeFileName(songName);
        String SONG_DIR;
        if (albumName != null && !albumName.trim().isEmpty()) {
            String safeAlbumName = sanitizeFileName(albumName);
            SONG_DIR = artistDir + "albums/" + safeAlbumName + "/" + safeSongName + "/";
            ensureDataDirectoryExists(SONG_DIR);

            File songFileCheck = new File(SONG_DIR, safeSongName + ".txt");
            if (songFileCheck.exists()) {
                showError("A song with the name '" + songName + "' already exists in this album!");
                return;
            }
        } else {
            SONG_DIR = artistDir + "singles/" + safeSongName + "/";
            ensureDataDirectoryExists(SONG_DIR);

            File songFileCheck = new File(SONG_DIR, safeSongName + ".txt");
            if (songFileCheck.exists()) {
                showError("A song with the name '" + songName + "' already exists as a single!");
                return;
            }
        }
        File songFile = new File(SONG_DIR, safeSongName + ".txt");
        List<String> songData = Arrays.asList(
                "Song Name: " + safeSongName,
                "Likes: 0",
                "Views: 0",
                "Release Date: Not set",
                "Lyrics: " + songDetails);
        try {
            Files.write(songFile.toPath(), songData, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("Error saving song data: " + e.getMessage());
            showError("Failed to save song data!");
        }
    }

    // Reading user information from file
    public static String[] readUser(String email, String nickName) {
        String safeNickName = sanitizeFileName(nickName);

        String USER_DIR = DATA_DIR + "users/" + safeNickName + "/";
        String userFile = USER_DIR + safeNickName + "-" + email + ".txt";

        File file = new File(userFile);
        if (!file.exists()) return null;

        String[] userData = new String[3];

        try {
            List<String> lines = Files.readAllLines(Paths.get(userFile));

            for (String line : lines) {
                int index = line.indexOf(": ");
                if (index != -1) {
                    String key = line.substring(0, index);
                    String value = line.substring(index + 2);
                    switch (key) {
                        case "Email":
                            userData[0] = value;
                            break;
                        case "Nickname":
                            userData[1] = value;
                            break;
                        case "Password":
                            userData[2] = value;
                            break;
                    }
                }
            }

            if (userData[0] == null || userData[1] == null || userData[2] == null) {
                System.err.println("User data is incomplete: " + userFile);
                showError("User data is corrupted!");
                return null;
            }

        } catch (IOException e) {
            System.err.println("Error reading user data: " + e.getMessage());
            showError("Failed to read user data!");
            return null;
        }
        return userData;
    }


    public static synchronized void saveFollowingArtists(String nickName, List<Artist> followingArtists) {
        String safeNickName = sanitizeFileName(nickName);

        String USER_DIR = DATA_DIR + "users/" + safeNickName + "/";
        String fileName = USER_DIR + "followings.txt";
        ensureDataDirectoryExists(USER_DIR);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            if (followingArtists.isEmpty()) {
                writer.write("Following: (None)");
            } else {
                Set<String> artistNames = followingArtists.stream()
                        .map(Artist::getNickName)
                        .collect(Collectors.toSet());
                writer.write("Following:" + String.join(",", artistNames));
            }
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing followings file: " + e.getMessage());
            showError("Failed to save following artists!");
        }
    }

    // Load user follows from file
    public static List<Artist> loadFollowingArtists(String nickName, List<Artist> allArtists) {
        String safeNickName = sanitizeFileName(nickName);
        String userDir = DATA_DIR + "users/" + safeNickName + "/";
        String fileName = userDir + "followings.txt";
        File followFile = new File(fileName);
        if (!followFile.exists()) return new ArrayList<>();
        List<String> userData = readFile(fileName);
        List<Artist> followingArtists = new ArrayList<>();
        Map<String, Artist> artistMap = allArtists.stream()
                .collect(Collectors.toMap(Artist::getNickName, a -> a, (a1, a2) -> a1));
        for (String line : userData) {
            if (line.startsWith("Following:")) {
                String[] artistNames = line.replace("Following:", "").split(",");
                for (String artistName : artistNames) {
                    String trimmedName = artistName.trim();
                    if (!trimmedName.isEmpty() && !" (None)".equals(trimmedName)) {
                        Artist artist = artistMap.get(trimmedName);
                        if (artist != null) followingArtists.add(artist);
                    }
                }
            }
        }
        return followingArtists;
    }

    public static synchronized void saveFollowers(Artist artist, List<User> followers) {
        String safeNickName = sanitizeFileName(artist.getNickName());

        String ARTIST_DIR = DATA_DIR + "artists/" + safeNickName + "/";
        String fileName = ARTIST_DIR + "followers.txt";
        ensureDataDirectoryExists(ARTIST_DIR);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            if (followers.isEmpty()) {
                writer.write("Followers: (None)");
            } else {
                writer.write("Followers:" + followers.stream()
                        .map(User::getNickName)
                        .collect(Collectors.joining(",")));
            }
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing followers file: " + e.getMessage());
            showError("Failed to save followers!");
        }
    }

    public static List<User> loadFollowers(Artist artist, List<User> allUsers) {
        String safeNickName = sanitizeFileName(artist.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        String fileName = artistDir + "followers.txt";
        File followFile = new File(fileName);
        if (!followFile.exists()) return new ArrayList<>();
        List<String> userData = readFile(fileName);
        List<User> followers = new ArrayList<>();
        Map<String, User> userMap = allUsers.stream()
                .collect(Collectors.toMap(User::getNickName, u -> u, (u1, u2) -> u1));
        for (String line : userData) {
            if (line.startsWith("Followers:")) {
                String[] followerNames = line.replace("Followers:", "").split(",");
                for (String followerName : followerNames) {
                    String trimmedName = followerName.trim();
                    if (!trimmedName.isEmpty() && !" (None)".equals(trimmedName)) {
                        User user = userMap.get(trimmedName);
                        if (user != null) followers.add(user);
                    }
                }
            }
        }
        return followers;
    }

    // Generic method for reading files
    public static List<String> readFile(String fileName) {
        List<String> lines = new ArrayList<>();
        File file = new File(fileName);

        if (!file.exists()) {
            System.err.println("File not found: " + fileName);
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName + " - " + e.getMessage());
            showError("Failed to read file: " + fileName);
        }

        return lines;
    }
}
