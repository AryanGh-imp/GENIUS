package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
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
import java.util.List;

public class SongAndAlbumDetailsController {

    @FXML private Label welcomeLabel;
    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label releaseDateLabel;
    @FXML private Label viewsLabel;
    @FXML private Label likesLabel;
    @FXML private Label songsLabel;
    @FXML private TextArea lyricsArea;
    @FXML private Button requestLyricsEditButton;
    @FXML private ListView<String> albumSongsListView;
    @FXML private ListView<Comment> commentsListView;
    @FXML private TextField commentField;
    @FXML private Button submitCommentButton;
    @FXML private ImageView coverImageView;
    @FXML private Button signOutButton;
    @FXML private Button likeButton;

    private UserMenuBarHandler menuBarHandler;
    private final SongFileManager songFileManager = new SongFileManager();
    private final SearchAndChartManager searchManager = new SearchAndChartManager(new ArtistFileManager(), new SongFileManager());

    @FXML
    public void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUsername() + "!");
        loadDetails();
        setupAlbumSongsListView();
        setupCommentsListView();
    }

    private void loadDetails() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        artistLabel.setText("Artist: " + artistName);

        lyricsArea.setVisible(false);
        requestLyricsEditButton.setVisible(false);
        likesLabel.setVisible(false);
        likeButton.setVisible(false);

        if (songTitle != null) {
            titleLabel.setText(songTitle);
            File songFile = getSongFile(artistName, songTitle, albumTitle);
            if (songFile.exists()) {
                Song song = loadAndProcessSong(songFile, albumTitle, artistName);
                updateSongDetails(song);
                loadCoverImage(songFile.getParent());
                commentsListView.getItems().setAll(songFileManager.loadComments(artistName, songTitle, albumTitle));
            }
        } else if (albumTitle != null) {
            titleLabel.setText(albumTitle);
            albumLabel.setVisible(false);
            songsLabel.setVisible(true);
            albumSongsListView.setVisible(true);

            File albumFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle + "/album.txt");
            if (albumFile.exists()) {
                List<String> albumData = loadFileData(albumFile.getPath());
                for (String line : albumData) {
                    if (line.startsWith("Release Date: ")) {
                        releaseDateLabel.setText("Release Date: " + line.substring("Release Date: ".length()));
                    } else if (line.startsWith("Views: ")) {
                        viewsLabel.setText("Views: " + line.substring("Views: ".length()));
                    }
                }

                loadCoverImage(albumFile.getParent());
                commentsListView.getItems().setAll(songFileManager.loadComments(artistName, null, albumTitle));

                String songsLine = albumData.stream()
                        .filter(line -> line.startsWith("Songs: "))
                        .findFirst()
                        .orElse("Songs: ");
                String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
                for (String title : songTitles) {
                    if (!title.trim().isEmpty()) {
                        albumSongsListView.getItems().add(title.trim());
                    }
                }
            }
        }
    }

    protected File getSongFile(String artistName, String songTitle, String albumTitle) {
        File songFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/singles/" + songTitle + "/" + songTitle + ".txt");
        if (!songFile.exists() && albumTitle != null) {
            songFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle + "/" + songTitle + "/" + songTitle + ".txt");
        }
        return songFile;
    }

    protected Song loadAndProcessSong(File songFile, String albumTitle, String artistName) {
        List<String> songData = loadFileData(songFile.getPath());
        String lyrics = searchManager.loadLyrics(songFile.getPath());
        // Default values for email and password
        Artist artist = new Artist("", artistName, "defaultPassword");
        return songFileManager.parseSongFromFile(songData, albumTitle != null ? new Album(albumTitle, "Not set", artist) : null, lyrics);
    }

    protected void incrementViewsForItem(String artistName, String songTitle, String albumTitle, boolean isAlbum) {
        String basePath = FileUtil.DATA_DIR + "artists/" + artistName + (albumTitle != null ? "/albums/" + albumTitle : "/singles/");
        if (isAlbum) {
            File albumFile = new File(basePath + "/album.txt");
            if (albumFile.exists()) {
                List<String> albumData = loadFileData(albumFile.getPath());
                String songsLine = albumData.stream()
                        .filter(line -> line.startsWith("Songs: "))
                        .findFirst()
                        .orElse("Songs: ");
                String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
                for (String title : songTitles) {
                    if (!title.trim().isEmpty()) {
                        File songFile = new File(basePath + "/" + title.trim() + "/" + title.trim() + ".txt");
                        if (songFile.exists()) {
                            Song song = loadAndProcessSong(songFile, albumTitle, artistName);
                            song.incrementViews();
                        }
                    }
                }
            }
        } else {
            File songFile = new File(basePath + songTitle + "/" + songTitle + ".txt");
            if (songFile.exists()) {
                Song song = loadAndProcessSong(songFile, albumTitle, artistName);
                song.incrementViews();
            }
        }
    }

    protected List<String> loadFileData(String filePath) {
        try {
            return FileUtil.readFile(filePath);
        } catch (Exception e) {
            System.err.println("Error loading file data: " + e.getMessage());
            return List.of();
        }
    }

    private void loadCoverImage(String directoryPath) {
        File imageFile = new File(directoryPath, "cover.png");
        if (imageFile.exists()) {
            try {
                coverImageView.setImage(new Image(imageFile.toURI().toString()));
            } catch (Exception e) {
                System.err.println("Error loading cover image: " + e.getMessage());
                coverImageView.setImage(new Image("GENIUS/src/main/resources/pics/Genius.com_logo_yellow.png"));
            }
        } else {
            coverImageView.setImage(new Image("GENIUS/src/main/resources/pics/Genius.com_logo_yellow.png"));
        }
    }

    private void updateSongDetails(Song song) {
        releaseDateLabel.setText("Release Date: " + song.getReleaseDate());
        viewsLabel.setText("Views: " + song.getViews());
        likesLabel.setText("Likes: " + song.getLikes());
        lyricsArea.setText(song.getLyrics());

        lyricsArea.setVisible(true);
        requestLyricsEditButton.setVisible(true);
        likesLabel.setVisible(true);
        likeButton.setVisible(true);
    }

    private void setupAlbumSongsListView() {
        albumSongsListView.setOnMouseClicked(event -> {
            String selectedSong = albumSongsListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                SessionManager.getInstance().setSelectedSong(selectedSong);
                loadDetails();
            }
        });
    }

    private void setupCommentsListView() {
        commentsListView.setCellFactory(param -> new ListCell<Comment>() {
            @Override
            protected void updateItem(Comment comment, boolean empty) {
                super.updateItem(comment, empty);
                if (empty || comment == null) {
                    setText(null);
                } else {
                    setText(comment.toString());
                }
            }
        });
    }

    @FXML
    public void submitComment() {
        String commentText = commentField.getText().trim();
        if (commentText.isEmpty()) {
            return;
        }

        String username = SessionManager.getInstance().getCurrentUsername();
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        songFileManager.addComment(artistName, songTitle, albumTitle, commentText, username);

        List<Comment> comments = songFileManager.loadComments(artistName, songTitle, albumTitle);
        commentsListView.getItems().setAll(comments);
        commentField.clear();
    }

    @FXML
    public void requestLyricsEdit() {
        SceneUtil.changeScene(requestLyricsEditButton, "/FXML-files/user/RequestLyricsEdit.fxml");
    }

    @FXML
    public void likeSong() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        if (songTitle != null) {
            File songFile = getSongFile(artistName, songTitle, albumTitle);
            if (songFile.exists()) {
                Song song = loadAndProcessSong(songFile, albumTitle, artistName);
                song.incrementLikes();
                updateSongDetails(song);
            }
        }
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}