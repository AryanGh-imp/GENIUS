package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import models.music.Album;
import models.music.Comment;
import models.music.Song;
import models.account.Artist;
import services.SearchAndChartManager;
import services.SessionManager;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import utils.FileUtil;
import utils.SceneUtil;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static services.AccountManager.validateEmail;
import static services.SessionManager.validateSession;

public class SongAndAlbumDetailsController extends BaseUserController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label releaseDateLabel;
    @FXML private Label viewsLabel;
    @FXML private Label likesLabel;
    @FXML private Label songsLabel;
    @FXML private Label lyricsLabel;
    @FXML private TextArea lyricsArea;
    @FXML private Button requestLyricsEditButton;
    @FXML private ListView<String> albumSongsListView;
    @FXML private ListView<Comment> commentsListView;
    @FXML private TextField commentField;
    @FXML private Button submitCommentButton;
    @FXML private ImageView coverImageView;
    @FXML private Button likeButton;

    private final SongFileManager songFileManager = new SongFileManager();
    private static final ArtistFileManager artistFileManager = new ArtistFileManager();
    private final SearchAndChartManager searchManager = new SearchAndChartManager(new ArtistFileManager(), new SongFileManager());
    private final Map<String, Song> songCache = new HashMap<>();

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        validateSession();
        initializeComponents();
        loadDetails();
        setupAlbumSongsListView();
        setupCommentsListView();
    }

    private void initializeComponents() {
        checkComponent(titleLabel, "titleLabel");
        checkComponent(artistLabel, "artistLabel");
        checkComponent(albumLabel, "albumLabel");
        checkComponent(releaseDateLabel, "releaseDateLabel");
        checkComponent(viewsLabel, "viewsLabel");
        checkComponent(likesLabel, "likesLabel");
        checkComponent(songsLabel, "songsLabel");
        checkComponent(lyricsLabel, "lyricsLabel");
        checkComponent(lyricsArea, "lyricsArea");
        checkComponent(requestLyricsEditButton, "requestLyricsEditButton");
        checkComponent(albumSongsListView, "albumSongsListView");
        checkComponent(commentsListView, "commentsListView");
        checkComponent(commentField, "commentField");
        checkComponent(submitCommentButton, "submitCommentButton");
        checkComponent(coverImageView, "coverImageView");
        checkComponent(likeButton, "likeButton");

        if (lyricsLabel != null) lyricsLabel.setVisible(false);
        if (lyricsArea != null) lyricsArea.setVisible(false);
        if (requestLyricsEditButton != null) requestLyricsEditButton.setVisible(false);
        if (likesLabel != null) likesLabel.setVisible(false);
        if (likeButton != null) likeButton.setVisible(false);
        if (albumLabel != null) albumLabel.setVisible(false);
        if (songsLabel != null) songsLabel.setVisible(false);
        if (albumSongsListView != null) albumSongsListView.setVisible(false);
    }

    void loadDetails() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();
        String artistEmail = SessionManager.getInstance().getSelectedArtistEmail();

        System.out.println("loadDetails - Artist: " + artistName + ", Song: " + songTitle + ", Album: " + albumTitle + ", Email: " + artistEmail);

        if (artistName != null && (artistEmail == null || artistEmail.trim().isEmpty())) {
            artistEmail = artistFileManager.findEmailByNickName(artistName, "artist");
            if (artistEmail == null) {
                System.err.println("Failed to find email for artist: " + artistName + ". Checking alternate methods...");
                artistEmail = fallbackFindEmail(artistName);
            }
            if (artistEmail != null) {
                SessionManager.getInstance().setSelectedArtistEmail(artistEmail);
                System.out.println("Recovered artist email in loadDetails: " + artistEmail);
            } else {
                System.err.println("Critical: No email found for artist: " + artistName + ". Proceeding with limited functionality.");
            }
        }

        if (artistLabel != null) {
            artistLabel.setText("Artist: " + (artistName != null ? artistName : "Unknown"));
        }

        if (songTitle != null && !songTitle.trim().isEmpty() && artistName != null) {
            loadSongDetails(artistName, songTitle, albumTitle);
        } else if (albumTitle != null && artistName != null) {
            loadAlbumDetails(artistName, albumTitle);
        } else {
            System.err.println("Insufficient data to load details: artistName=" + artistName +
                    ", songTitle=" + songTitle + ", albumTitle=" + albumTitle);
            if (titleLabel != null) {
                titleLabel.setText("Error: Insufficient data to load details");
            }
        }
    }

    private String fallbackFindEmail(String artistName) {
        File artistDir = new File(FileUtil.DATA_DIR + "artists/" + artistName);
        if (artistDir.exists()) {
            File[] files = artistDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".txt") && !file.getName().equals("index_artists.txt")) {
                        List<String> data = loadFileData(file.getPath());
                        for (String line : data) {
                            if (line.startsWith("Email: ")) {
                                return line.substring("E mail: ".length()).trim();
                            }
                        }
                    }
                }
            }
        }
        System.err.println("Could not recover email for artist: " + artistName);
        return null;
    }

    private void loadSongDetails(String artistName, String songTitle, String albumTitle) {
        if (titleLabel != null) {
            titleLabel.setText(songTitle);
        }

        // Validate if the song belongs to the specified album (if any)
        String actualAlbumTitle = albumTitle;
        if (albumTitle != null) {
            File albumFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle + "/album.txt");
            if (albumFile.exists()) {
                List<String> albumData = loadFileData(albumFile.getPath());
                String songsLine = albumData.stream()
                        .filter(line -> line.startsWith("Songs: "))
                        .findFirst()
                        .orElse("Songs: ");
                if (!songsLine.contains(songTitle)) {
                    // Song is not in this album, treat it as a single
                    actualAlbumTitle = null;
                    SessionManager.getInstance().setSelectedAlbum(null);
                }
            } else {
                // Album doesn't exist, treat the song as a single
                actualAlbumTitle = null;
                SessionManager.getInstance().setSelectedAlbum(null);
            }
        }

        File songFile = getSongFile(artistName, songTitle, actualAlbumTitle);
        if (songFile.exists()) {
            Song song = loadAndProcessSong(songFile, actualAlbumTitle, artistName);
            updateSongDetails(song);
            String imagePath = song.getAlbumArtPath();
            if (imagePath != null && new File(imagePath).exists()) {
                loadImage(coverImageView, imagePath);
            } else {
                loadImage(coverImageView, songFile.getParent() + "/song_art.jpg");
            }
            if (commentsListView != null) {
                commentsListView.getItems().setAll(songFileManager.loadComments(artistName, songTitle, actualAlbumTitle));
            }

            if (albumSongsListView != null) {
                albumSongsListView.setVisible(false);
                albumSongsListView.setManaged(false);
            }
            if (songsLabel != null) {
                songsLabel.setVisible(false);
                songsLabel.setManaged(false);
            }
        } else {
            System.err.println("Song file does not exist: " + songFile.getPath());
        }
    }

    private void loadAlbumDetails(String artistName, String albumTitle) {
        SessionManager.getInstance().setSelectedSong(null);

        if (titleLabel != null) titleLabel.setText(albumTitle);
        if (albumLabel != null) albumLabel.setVisible(false);
        if (songsLabel != null) {
            songsLabel.setVisible(true);
            songsLabel.setManaged(true);
        }
        if (albumSongsListView != null) {
            albumSongsListView.setVisible(true);
            albumSongsListView.setManaged(true);
        }
        if (lyricsLabel != null) {
            lyricsLabel.setVisible(false);
            lyricsLabel.setManaged(false);
        }
        if (lyricsArea != null) {
            lyricsArea.setVisible(false);
            lyricsArea.setManaged(false);
            lyricsArea.setText("");
        }
        if (requestLyricsEditButton != null) requestLyricsEditButton.setVisible(false);

        File albumFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle + "/album.txt");
        if (albumFile.exists()) {
            List<String> albumData = loadFileData(albumFile.getPath());
            updateAlbumMetadata(albumData);
            loadImage(coverImageView, albumFile.getParent() + "/album_art.jpg");
            if (commentsListView != null) {
                commentsListView.getItems().setAll(songFileManager.loadAlbumComments(artistName, albumTitle));
            }
            loadAlbumSongs(albumData);
            int totalViews = calculateTotalAlbumViews(artistName, albumTitle, albumData);
            if (viewsLabel != null) viewsLabel.setText("Total Views: " + totalViews);
        } else {
            System.err.println("Album file does not exist: " + albumFile.getPath());
            if (commentsListView != null) commentsListView.getItems().clear();
        }
    }

    private int calculateTotalAlbumViews(String artistName, String albumTitle, List<String> albumData) {
        int totalViews = 0;
        String songsLine = albumData.stream()
                .filter(line -> line.startsWith("Songs: "))
                .findFirst()
                .orElse("Songs: ");
        String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
        String basePath = FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle;

        for (String title : songTitles) {
            if (!title.trim().isEmpty()) {
                File songFile = new File(basePath + "/" + title.trim() + "/" + title.trim() + ".txt");
                if (songFile.exists()) {
                    Song song = loadAndProcessSong(songFile, albumTitle, artistName);
                    totalViews += song.getViews();
                }
            }
        }
        return totalViews;
    }

    private void updateAlbumMetadata(List<String> albumData) {
        for (String line : albumData) {
            if (line.startsWith("Release Date: ")) {
                if (releaseDateLabel != null) {
                    releaseDateLabel.setText("Release Date: " + line.substring("Release Date: ".length()));
                }
            }
        }
    }

    private void loadAlbumSongs(List<String> albumData) {
        if (albumSongsListView == null) return;

        String songsLine = albumData.stream()
                .filter(line -> line.startsWith("Songs: "))
                .findFirst()
                .orElse("Songs: ");
        String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
        albumSongsListView.getItems().clear();
        for (String title : songTitles) {
            if (!title.trim().isEmpty()) {
                albumSongsListView.getItems().add(title.trim());
            }
        }
    }

    protected File getSongFile(String artistName, String songTitle, String albumTitle) {
        File songFile;
        File file = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/singles/" + songTitle + "/" + songTitle + ".txt");
        if (albumTitle != null) {
            songFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle + "/" + songTitle + "/" + songTitle + ".txt");
            if (!songFile.exists()) {
                // Fallback to singles directory if the song is not found in the album
                songFile = file;
                if (songFile.exists()) {
                    // Update SessionManager to reflect that this is a single
                    SessionManager.getInstance().setSelectedAlbum(null);
                }
            }
        } else {
            songFile = file;
        }
        if (!songFile.exists()) {
            System.err.println("Song file not found: " + songFile.getPath());
        }
        return songFile;
    }

    protected Song loadAndProcessSong(File songFile, String albumTitle, String artistName) {
        List<String> songData = loadFileData(songFile.getPath());
        String lyrics = searchManager.loadLyrics(songFile.getPath());
        String artistEmail = SessionManager.getInstance().getSelectedArtistEmail();

        if (artistEmail == null || artistEmail.trim().isEmpty()) {
            artistEmail = artistFileManager.findEmailByNickName(artistName, "artist");
            if (artistEmail != null) {
                SessionManager.getInstance().setSelectedArtistEmail(artistEmail);
                System.out.println("Recovered artist email in loadAndProcessSong: " + artistEmail);
            } else {
                System.err.println("Artist email not found for artist: " + artistName + ". Using default email.");
                artistEmail = "default@example.com";
            }
        }

        if (!validateEmail(artistEmail)) {
            System.err.println("Invalid email format for artist: " + artistName + ". Using default email.");
            artistEmail = "default@example.com";
        }

        Artist artist = artistFileManager.getArtistByNickName(artistName);
        if (artist == null) {
            artist = new Artist(artistEmail, artistName, "defaultPassword");
            System.out.println("Created new Artist instance for: " + artistName);
        }

        String cacheKey = songFile.getAbsolutePath() + (albumTitle != null ? "_" + albumTitle : "");
        Song song = songCache.get(cacheKey);
        if (song == null) {
            song = songFileManager.parseSongFromFile(songData, albumTitle != null ? new Album(albumTitle, "Not set", artist) : null, lyrics, artist);
            songCache.put(cacheKey, song);
        } else {
            Song updatedSong = songFileManager.parseSongFromFile(songData, albumTitle != null ? new Album(albumTitle, "Not set", artist) : null, lyrics, artist);
            song.setViews(updatedSong.getViews());
            song.setLikes(updatedSong.getLikes());
        }
        return song;
    }

    private void updateSongDetails(Song song) {
        if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: " + song.getReleaseDate());
        if (viewsLabel != null) viewsLabel.setText("Views: " + song.getViews());
        if (likesLabel != null) {
            likesLabel.setText("Likes: " + song.getLikes());
            likesLabel.setVisible(true);
        }
        if (lyricsLabel != null) {
            lyricsLabel.setVisible(true);
            lyricsLabel.setManaged(true);
        }
        if (lyricsArea != null) {
            lyricsArea.setText(song.getLyrics());
            lyricsArea.setVisible(true);
            lyricsArea.setManaged(true);
        }
        if (requestLyricsEditButton != null) requestLyricsEditButton.setVisible(true);
        if (likeButton != null) likeButton.setVisible(true);
    }

    private void setupAlbumSongsListView() {
        if (albumSongsListView == null) return;
        setupListView(albumSongsListView, "/FXML-files/user/SongAndAlbumDetails.fxml", "song");
    }

    private void setupCommentsListView() {
        if (commentsListView == null) return;
        commentsListView.setCellFactory(param -> new ListCell<Comment>() {
            @Override
            protected void updateItem(Comment comment, boolean empty) {
                super.updateItem(comment, empty);
                setText(empty || comment == null ? null : comment.toString());
            }
        });
    }

    @FXML
    public void submitComment() {
        if (commentField == null || commentsListView == null) return;

        String commentText = commentField.getText().trim();
        if (commentText.isEmpty()) return;

        String username = SessionManager.getInstance().getCurrentUsername();
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        if (songTitle != null && !songTitle.trim().isEmpty()) {
            songFileManager.addComment(artistName, songTitle, albumTitle, commentText, username);
            commentsListView.getItems().setAll(songFileManager.loadComments(artistName, songTitle, albumTitle));
        } else if (albumTitle != null && !albumTitle.trim().isEmpty()) {
            songFileManager.addAlbumComment(artistName, albumTitle, commentText, username);
            commentsListView.getItems().setAll(songFileManager.loadAlbumComments(artistName, albumTitle));
        }

        commentField.clear();
    }

    @FXML
    public void requestLyricsEdit() {
        if (requestLyricsEditButton != null) {
            SceneUtil.changeScene(requestLyricsEditButton, "/FXML-files/user/RequestLyricsEdit.fxml");
        }
    }

    @FXML
    public void likeSong() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        if (songTitle != null) {
            File songFile = getSongFile(artistName, songTitle, albumTitle);
            if (songFile.exists()) {
                String actualAlbumTitle = null;
                if (songFile.getPath().contains("albums")) {
                    actualAlbumTitle = albumTitle;
                }

                Song song = loadAndProcessSong(songFile, actualAlbumTitle, artistName);
                int oldViews = song.getViews();
                song.incrementLikes();
                String cacheKey = songFile.getAbsolutePath() + "_" + actualAlbumTitle;
                songCache.put(cacheKey, song);
                updateSongDetails(song);

                song.setViews(oldViews);
                songFileManager.saveSong(Collections.singletonList(artistName), songTitle, actualAlbumTitle, song.getLyrics(), song.getReleaseDate(), song.getLikes(), song.getViews(), song.getAlbumArtPath());
                System.out.println("Likes updated and saved for song: " + songTitle + ", New Likes: " + song.getLikes() + ", Views (unchanged): " + song.getViews() + ", Album: " + actualAlbumTitle);
            }
        }
    }

    public void clearCache() {
        songCache.clear();
    }
}