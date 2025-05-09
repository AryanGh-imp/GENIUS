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
import java.util.List;

public class SongAndAlbumDetailsController extends BaseUserController {

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
    @FXML private Button likeButton;

    private final SongFileManager songFileManager = new SongFileManager();
    private final SearchAndChartManager searchManager = new SearchAndChartManager(new ArtistFileManager(), new SongFileManager());

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        loadDetails();
        setupAlbumSongsListView();
        setupCommentsListView();
    }

    private void loadDetails() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        checkComponent(artistLabel, "artistLabel");
        if (artistLabel != null) artistLabel.setText("Artist: " + artistName);

        checkComponent(lyricsArea, "lyricsArea");
        checkComponent(requestLyricsEditButton, "requestLyricsEditButton");
        checkComponent(likesLabel, "likesLabel");
        checkComponent(likeButton, "likeButton");
        if (lyricsArea != null) lyricsArea.setVisible(false);
        if (requestLyricsEditButton != null) requestLyricsEditButton.setVisible(false);
        if (likesLabel != null) likesLabel.setVisible(false);
        if (likeButton != null) likeButton.setVisible(false);

        if (songTitle != null) {
            loadSongDetails(artistName, songTitle, albumTitle);
        } else if (albumTitle != null) {
            loadAlbumDetails(artistName, albumTitle);
        }
    }

    private void loadSongDetails(String artistName, String songTitle, String albumTitle) {
        checkComponent(titleLabel, "titleLabel");
        if (titleLabel != null) titleLabel.setText(songTitle);

        File songFile = getSongFile(artistName, songTitle, albumTitle);
        if (songFile.exists()) {
            Song song = loadAndProcessSong(songFile, albumTitle, artistName);
            updateSongDetails(song);
            loadImage(coverImageView, songFile.getParent() + "/cover.png");
            checkComponent(commentsListView, "commentsListView");
            if (commentsListView != null) {
                commentsListView.getItems().setAll(songFileManager.loadComments(artistName, songTitle, albumTitle));
            }
        }
    }

    private void loadAlbumDetails(String artistName, String albumTitle) {
        checkComponent(titleLabel, "titleLabel");
        checkComponent(albumLabel, "albumLabel");
        checkComponent(songsLabel, "songsLabel");
        checkComponent(albumSongsListView, "albumSongsListView");
        if (titleLabel != null) titleLabel.setText(albumTitle);
        if (albumLabel != null) albumLabel.setVisible(false);
        if (songsLabel != null) songsLabel.setVisible(true);
        if (albumSongsListView != null) albumSongsListView.setVisible(true);

        File albumFile = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/" + albumTitle + "/album.txt");
        if (albumFile.exists()) {
            List<String> albumData = loadFileData(albumFile.getPath());
            updateAlbumMetadata(albumData);
            loadImage(coverImageView, albumFile.getParent() + "/cover.png");
            checkComponent(commentsListView, "commentsListView");
            if (commentsListView != null) {
                commentsListView.getItems().setAll(songFileManager.loadComments(artistName, null, albumTitle));
            }
            loadAlbumSongs(albumData);
        }
    }

    private void updateAlbumMetadata(List<String> albumData) {
        for (String line : albumData) {
            if (line.startsWith("Release Date: ")) {
                checkComponent(releaseDateLabel, "releaseDateLabel");
                if (releaseDateLabel != null) {
                    releaseDateLabel.setText("Release Date: " + line.substring("Release Date: ".length()));
                }
            } else if (line.startsWith("Views: ")) {
                checkComponent(viewsLabel, "viewsLabel");
                if (viewsLabel != null) {
                    viewsLabel.setText("Views: " + line.substring("Views: ".length()));
                }
            }
        }
    }

    private void loadAlbumSongs(List<String> albumData) {
        checkComponent(albumSongsListView, "albumSongsListView");
        if (albumSongsListView == null) return;

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

    private void updateSongDetails(Song song) {
        checkComponent(releaseDateLabel, "releaseDateLabel");
        checkComponent(viewsLabel, "viewsLabel");
        checkComponent(likesLabel, "likesLabel");
        checkComponent(lyricsArea, "lyricsArea");
        checkComponent(requestLyricsEditButton, "requestLyricsEditButton");
        checkComponent(likeButton, "likeButton");

        if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: " + song.getReleaseDate());
        if (viewsLabel != null) viewsLabel.setText("Views: " + song.getViews());
        if (likesLabel != null) {
            likesLabel.setText("Likes: " + song.getLikes());
            likesLabel.setVisible(true);
        }
        if (lyricsArea != null) {
            lyricsArea.setText(song.getLyrics());
            lyricsArea.setVisible(true);
        }
        if (requestLyricsEditButton != null) requestLyricsEditButton.setVisible(true);
        if (likeButton != null) likeButton.setVisible(true);
    }

    private void setupAlbumSongsListView() {
        checkComponent(albumSongsListView, "albumSongsListView");
        if (albumSongsListView == null) return;

        albumSongsListView.setOnMouseClicked(event -> {
            String selectedSong = albumSongsListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                SessionManager.getInstance().setSelectedSong(selectedSong);
                loadDetails();
            }
        });
    }

    private void setupCommentsListView() {
        checkComponent(commentsListView, "commentsListView");
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
        checkComponent(commentField, "commentField");
        checkComponent(commentsListView, "commentsListView");
        if (commentField == null || commentsListView == null) return;

        String commentText = commentField.getText().trim();
        if (commentText.isEmpty()) return;

        String username = SessionManager.getInstance().getCurrentUsername();
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        songFileManager.addComment(artistName, songTitle, albumTitle, commentText, username);
        commentsListView.getItems().setAll(songFileManager.loadComments(artistName, songTitle, albumTitle));
        commentField.clear();
    }

    @FXML
    public void requestLyricsEdit() {
        checkComponent(requestLyricsEditButton, "requestLyricsEditButton");
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
                Song song = loadAndProcessSong(songFile, albumTitle, artistName);
                song.incrementLikes();
                updateSongDetails(song);
            }
        }
    }
}