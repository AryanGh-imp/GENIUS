package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import models.DTO.SearchResultDTO;
import models.DTO.SongDTO;
import services.SearchAndChartManager;
import services.SessionManager;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.SceneUtil;

import java.util.List;

public class SearchPageController extends BaseUserController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ListView<String> searchResultsListView;

    private final SearchAndChartManager searchManager;
    private final SongAndAlbumDetailsController detailsHelper = new SongAndAlbumDetailsController();

    public SearchPageController() {
        this.searchManager = new SearchAndChartManager(new ArtistFileManager(), new SongFileManager());
    }

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        setupSearchResultsListView();
    }

    @FXML
    public void search() {
        checkComponent(searchField, "searchField");
        checkComponent(searchResultsListView, "searchResultsListView");
        if (searchField == null || searchResultsListView == null) return;

        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            AlertUtil.showError("Please enter a search query.");
            return;
        }

        searchResultsListView.getItems().clear();
        List<SearchResultDTO> results = searchManager.search(query);

        if (results.isEmpty()) {
            searchResultsListView.getItems().add("No results found for \"" + query + "\"");
        } else {
            searchResultsListView.getItems().addAll(results.stream()
                    .map(this::formatSearchResult)
                    .toList());
        }
    }

    private String formatSearchResult(SearchResultDTO result) {
        return switch (result.type()) {
            case ARTIST -> "Artist: " + result.name();
            case ALBUM -> "Album: " + result.name() + " - " + searchManager.getAllSongs().stream()
                    .filter(song -> song.albumName() != null && song.albumName().equals(result.name()))
                    .findFirst()
                    .map(SongDTO::artistName)
                    .orElse("");
            case SONG -> {
                String artistName = searchManager.getAllSongs().stream()
                        .filter(song -> song.title().equals(result.name()))
                        .findFirst()
                        .map(SongDTO::artistName)
                        .orElse("");
                String albumName = searchManager.getAllSongs().stream()
                        .filter(song -> song.title().equals(result.name()) && song.artistName().equals(artistName))
                        .findFirst()
                        .map(song -> song.albumName() != null ? song.albumName() : null)
                        .orElse(null);
                yield "Song: " + result.name() + " - " + artistName + (albumName != null ? " (Album: " + albumName + ")" : " (Single)");
            }
            default -> "";
        };
    }

    private void setupSearchResultsListView() {
        checkComponent(searchResultsListView, "searchResultsListView");
        if (searchResultsListView == null) return;

        searchResultsListView.setOnMouseClicked(event -> {
            String selectedItem = searchResultsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.startsWith("Artist: ")) {
                    String artistName = selectedItem.substring("Artist: ".length());
                    SessionManager.getInstance().setSelectedArtist(artistName);
                    SceneUtil.changeScene(searchResultsListView, "/FXML-files/user/ArtistProfile.fxml");
                } else if (selectedItem.startsWith("Song: ")) {
                    String songInfo = selectedItem.substring("Song: ".length());
                    String[] parts = songInfo.split(" - ");
                    String songTitle = parts[0];
                    String artistName = parts[1].split(" \\(")[0];
                    String albumTitle = songInfo.contains("(Album: ") ? songInfo.substring(songInfo.indexOf("(Album: ") + "(Album: ".length(), songInfo.length() - 1) : null;

                    detailsHelper.incrementViewsForItem(artistName, songTitle, albumTitle, false);

                    SessionManager.getInstance().setSelectedSong(songTitle);
                    SessionManager.getInstance().setSelectedArtist(artistName);
                    SessionManager.getInstance().setSelectedAlbum(albumTitle);
                    SceneUtil.changeScene(searchResultsListView, "/FXML-files/user/SongAndAlbumDetails.fxml");
                } else if (selectedItem.startsWith("Album: ")) {
                    String albumInfo = selectedItem.substring("Album: ".length());
                    String[] parts = albumInfo.split(" - ");
                    String albumTitle = parts[0];
                    String artistName = parts[1];

                    detailsHelper.incrementViewsForItem(artistName, null, albumTitle, true);

                    SessionManager.getInstance().setSelectedArtist(artistName);
                    SessionManager.getInstance().setSelectedAlbum(albumTitle);
                    SessionManager.getInstance().setSelectedSong(null);
                    SceneUtil.changeScene(searchResultsListView, "/FXML-files/user/SongAndAlbumDetails.fxml");
                }
            }
        });
    }
}